package com.seo.keywordgenerator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hybrid trend validation:
 * - Redis cache first
 * - Google Autocomplete suggest endpoint for “is searched?”
 * - Pytrends popularity is expected to be pre-fetched into Redis by a daily job
 *
 * Performance guards:
 * - rate limit external autocomplete fetches
 * - short TTL for cache-miss values
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HybridTrendService implements TrendService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @Value("${app.trends.cache-ttl-real-time:PT6H}")
    private Duration realTimeCacheTtl;

    @Value("${app.trends.cache-ttl-pytrends:PT24H}")
    private Duration pytrendsCacheTtl;

    @Value("${app.trends.autocomplete.rate-limit-per-minute:10}")
    private int autocompleteRateLimitPerMinute;

    @Value("${app.trends.autocomplete.timeout-ms:1500}")
    private int autocompleteTimeoutMs;

    private static final String AUTOCOMPLETE_CACHE_PREFIX = "trend:autocomplete:";
    private static final String TRENDDATA_CACHE_PREFIX = "trend:data:";
    private static final String PYTRENDS_CACHE_PREFIX = "trend:pyt:";

    // Simple in-memory rate limiter for autocomplete calls (per instance)
    private final ConcurrentHashMap<String, RecentCounter> autocompleteCounters = new ConcurrentHashMap<>();

    @Override
    public Optional<TrendData> getTrendData(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalized = keyword.toLowerCase().trim();
        String cacheKey = TRENDDATA_CACHE_PREFIX + normalized;

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof TrendData td) {
                return Optional.of(td);
            }
        } catch (Exception e) {
            log.debug("Redis read failed for keyword {}: {}", keyword, e.getMessage());
        }

        // External fetch (limited)
        TrendData computed = fetchAndComputeTrendData(normalized);

        try {
            redisTemplate.opsForValue().set(cacheKey, computed, realTimeCacheTtl);
        } catch (Exception e) {
            log.debug("Redis write failed for keyword {}: {}", keyword, e.getMessage());
        }

        return Optional.ofNullable(computed);
    }

    @Override
    public Optional<TrendData> getTrendData(String keyword, LocalDateTime startDate, LocalDateTime endDate) {
        // Keep simple: ignore date range for now; the score is precomputed & cached.
        // Could be extended by storing per-week/month popularity buckets.
        return getTrendData(keyword);
    }

    @Override
    public Map<String, TrendData> getBatchTrendData(java.util.List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return Collections.emptyMap();

        Map<String, TrendData> out = new HashMap<>();
        for (String kw : keywords) {
            getTrendData(kw).ifPresent(td -> out.put(td.keyword(), td));
        }
        return out;
    }

    @Override
    public boolean isTrending(String keyword) {
        return getTrendData(keyword).map(td -> td.trendDirection() > 0.1).orElse(false);
    }

    @Override
    public double getPopularityScore(String keyword) {
        return getTrendData(keyword).map(TrendData::popularityScore).orElse(0.0);
    }

    @Override
    public double getTrendScore(String keyword) {
        // In this implementation trendScore == popularityScore.
        return getPopularityScore(keyword);
    }

    @Override
    public void cacheTrendData(String keyword, TrendData trendData) {
        if (keyword == null || trendData == null) return;
        String normalized = keyword.toLowerCase().trim();
        String cacheKey = TRENDDATA_CACHE_PREFIX + normalized;
        try {
            redisTemplate.opsForValue().set(cacheKey, trendData, realTimeCacheTtl);
        } catch (Exception e) {
            log.debug("Redis write failed in cacheTrendData for {}: {}", keyword, e.getMessage());
        }
    }

    @Override
    public void invalidateCache(String keyword) {
        if (keyword == null) return;
        String normalized = keyword.toLowerCase().trim();
        String cacheKey = TRENDDATA_CACHE_PREFIX + normalized;
        try {
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            log.debug("Redis delete failed in invalidateCache for {}: {}", keyword, e.getMessage());
        }
    }

    private TrendData fetchAndComputeTrendData(String normalizedKeyword) {
        // 1) Google autocomplete: fast “searched?” proxy
        boolean searched = fetchAutocompleteSearched(normalizedKeyword);

        // 2) Pytrends popularity: expected to be filled by daily prefetch
        //    If absent, we degrade gracefully.
        Optional<Double> pytrendsPopularity = fetchPytrendsPopularity(normalizedKeyword);
        Double pytPopularity = pytrendsPopularity.orElse(null);

        double popularityScore;
        String source;
        boolean estimated;
        if (pytPopularity != null) {
            popularityScore = pytPopularity;
            source = "PYTRENDS_REDIS";
            estimated = false;
        } else {
            popularityScore = searched ? 55.0 : 8.0;

            if (containsAny(normalizedKeyword, "react", "javascript", "python", "java", "api", "node", "vue", "angular")) {
                popularityScore += 10.0;
            } else if (containsAny(normalizedKeyword, "tutorial", "guide", "learn", "course", "training")) {
                popularityScore += 7.0;
            } else if (containsAny(normalizedKeyword, "best", "top", "ultimate", "advanced", "professional")) {
                popularityScore += 5.0;
            }

            source = searched ? "GOOGLE_AUTOCOMPLETE" : "NO_TREND_DATA";
            estimated = true;
        }

        long searchVolume = estimated ? 0L : estimateSearchVolumeFromPopularity(popularityScore);

        // trendDirection: without pytrends time series, we approximate.
        // If autocomplete contains “rising intent” modifiers, bump it slightly.
        double trendDirection = searched ? estimateTrendDirectionFromIntent(normalizedKeyword) : -0.05;

        Map<String, Long> regionalData = defaultRegionalDistribution(searchVolume, normalizedKeyword);
        Map<String, Double> relatedKeywords = Map.of();

        return new TrendData(
                normalizedKeyword,
                clamp(popularityScore, 0.0, 100.0),
                Math.max(0L, searchVolume),
                clamp(trendDirection, -0.5, 0.5),
                LocalDateTime.now(),
                regionalData,
                relatedKeywords,
                source,
                estimated
        );
    }

    private boolean fetchAutocompleteSearched(String keyword) {
        String cacheKey = AUTOCOMPLETE_CACHE_PREFIX + keyword;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof Boolean b) return b;
            if (cached instanceof String s) return Boolean.parseBoolean(s);
        } catch (Exception e) {
            // ignore
        }

        if (!allowAutocompleteCall(keyword)) {
            // Rate limited: assume not searched.
            return false;
        }

        try {
            String url = "https://suggestqueries.google.com/complete/search?client=firefox&hl=en&q="
                    + URLEncoder.encode(keyword, StandardCharsets.UTF_8);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(autocompleteTimeoutMs))
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) return false;

            // Response is JSON array: [query, ["suggestion1", "suggestion2", ...], ...]
            // We do lightweight string matching instead of strict JSON parsing to avoid extra deps.
            String body = resp.body();
            boolean found = body != null && body.toLowerCase().contains(keyword);

            redisTemplate.opsForValue().set(cacheKey, found, Duration.ofHours(12));
            return found;
        } catch (Exception e) {
            log.debug("Autocomplete fetch failed for {}: {}", keyword, e.getMessage());
            return false;
        }
    }

    private Optional<Double> fetchPytrendsPopularity(String keyword) {
        String cacheKey = PYTRENDS_CACHE_PREFIX + keyword;
        try {
            Object v = redisTemplate.opsForValue().get(cacheKey);
            if (v instanceof Number n) {
                return Optional.of(n.doubleValue());
            }
            if (v instanceof String s) {
                return Optional.of(Double.parseDouble(s));
            }
        } catch (Exception e) {
            // ignore
        }
        return Optional.empty();
    }

    private boolean allowAutocompleteCall(String keyword) {
        // very simple token bucket per keyword+instance
        RecentCounter counter = autocompleteCounters.computeIfAbsent(keyword, k -> new RecentCounter());
        return counter.incrementAndCheck(autocompleteRateLimitPerMinute, Duration.ofMinutes(1));
    }

    private long estimateSearchVolumeFromPopularity(double popularityScore) {
        // Realistic volume estimation with variation
        double base = Math.max(1.0, popularityScore);
        
        // Add randomness to prevent identical volumes
        double randomFactor = 0.8 + (Math.random() * 0.4); // 0.8 to 1.2
        
        // Much more realistic volume multipliers based on popularity tiers
        long volume;
        if (popularityScore >= 80) {
            volume = (long) (base * randomFactor * 50 + Math.random() * 20); // High: 50-70
        } else if (popularityScore >= 60) {
            volume = (long) (base * randomFactor * 30 + Math.random() * 10); // Medium-high: 30-40
        } else if (popularityScore >= 40) {
            volume = (long) (base * randomFactor * 15 + Math.random() * 5); // Medium: 15-20
        } else {
            volume = (long) (base * randomFactor * 5 + Math.random() * 3); // Low: 5-8
        }
        
        return Math.max(5L, volume);
    }

    private double estimateTrendDirectionFromIntent(String keyword) {
        // tiny heuristic: “tutorial/guide/course/best/learn” queries often have rising intent.
        String k = keyword;
        double dir = 0.02;
        if (containsAny(k, "tutorial", "learn", "guide", "course", "training", "certification")) dir += 0.08;
        if (containsAny(k, "best", "top", "ultimate", "2025", "latest", "new")) dir += 0.05;
        return clamp(dir, -0.3, 0.5);
    }

    private Map<String, Long> defaultRegionalDistribution(long totalVolume, String keyword) {
        // Simple fixed weights; more advanced can be replaced by pytrends geo.
        double[] weights = {0.40, 0.15, 0.10, 0.07, 0.10, 0.08, 0.06, 0.04};
        String[] regions = {"US", "UK", "IN", "CA", "DE", "FR", "AU", "JP"};
        Map<String, Long> out = new HashMap<>();
        for (int i = 0; i < regions.length; i++) {
            long v = (long) (totalVolume * weights[i]);
            out.put(regions[i], Math.max(10L, v));
        }
        return out;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static boolean containsAny(String keyword, String... parts) {
        for (String p : parts) {
            if (p != null && !p.isBlank() && keyword.contains(p.toLowerCase())) return true;
        }
        return false;
    }

    private static final class RecentCounter {
        private long windowStartEpochMs = 0L;
        private int count = 0;

        synchronized boolean incrementAndCheck(int limit, Duration window) {
            long now = System.currentTimeMillis();
            long windowMs = window.toMillis();
            if (windowStartEpochMs == 0L || now - windowStartEpochMs >= windowMs) {
                windowStartEpochMs = now;
                count = 0;
            }
            count++;
            return count <= limit;
        }
    }
}

