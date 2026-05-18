package com.seo.keywordgenerator.scheduler;

import com.seo.keywordgenerator.dto.KeywordResponseDTO;
import com.seo.keywordgenerator.service.CacheService;
import com.seo.keywordgenerator.service.TrendService;
import lombok.RequiredArgsConstructor;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrendingKeywordScheduler {

    private final TrendService trendService;
    private final CacheService cacheService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${scheduler.trending.enabled:true}")
    private boolean schedulerEnabled;
    
    @Value("${scheduler.trending.cache-duration:PT24H}")
    private Duration cacheDuration;
    
    @Value("${scheduler.trending.max-keywords:100}")
    private int maxTrendingKeywords;

    @Value("${scheduler.trending.pytrends-batch-size:3}")
    private int pytrendsBatchSize;

    @Value("${scheduler.trending.pytrends-sleep-seconds:8.0}")
    private String pytrendsSleepSeconds;

    @Value("${scheduler.trending.python-command:python}")
    private String pythonCommand;

    @Value("${scheduler.trending.pytrends-script:backend/scripts/pytrends_prefetch.py}")
    private String pytrendsScript;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private String redisPort;

    @Value("${spring.data.redis.database:0}")
    private String redisDatabase;
    
    private static final String TRENDING_CACHE_KEY = "trending:keywords:daily";
    private static final String TRENDING_STATS_KEY = "trending:stats";
    private static final String LAST_RUN_KEY = "trending:last_run";
    
    private static final List<String> BASE_KEYWORDS = Arrays.asList(
            "react", "vue", "angular", "javascript", "typescript", "nodejs", "python", "java", "spring",
            "docker", "kubernetes", "aws", "azure", "gcp", "devops", "ci/cd", "microservices",
            "api", "rest", "graphql", "database", "sql", "nosql", "mongodb", "postgresql",
            "frontend", "backend", "fullstack", "mobile", "ios", "android", "flutter", "react native",
            "machine learning", "ai", "data science", "blockchain", "cybersecurity", "cloud computing",
            "testing", "unit testing", "integration testing", "e2e testing", "performance testing",
            "agile", "scrum", "kanban", "devops", "sre", "monitoring", "logging", "observability"
    );
    
    private static final List<String> TRENDING_MODIFIERS = Arrays.asList(
            "tutorial", "guide", "best practices", "examples", "course", "training", "certification",
            "for beginners", "advanced", "professional", "expert", "step by step", "complete guide",
            "2024", "2025", "latest", "new", "modern", "ultimate", "comprehensive"
    );

    @Scheduled(cron = "${scheduler.trending.cron:0 0 2 * * *}")
    public void fetchAndCacheTrendingKeywords() {
        runTrendingKeywordsFetch();
    }

    public TrendingPrefetchResult runTrendingKeywordsFetch() {
        if (!schedulerEnabled) {
            log.info("Trending keyword scheduler is disabled");
            return new TrendingPrefetchResult("disabled", 0, 0, "Scheduler is disabled");
        }

        log.info("Starting daily trending keywords fetch at {}", LocalDateTime.now());

        try {
            // 1) Pre-compute/cache “trending keywords” (cheap local variations)
            List<TrendingKeyword> trendingKeywords = fetchTrendingKeywords();
            cacheTrendingKeywords(trendingKeywords);
            updateTrendingStatistics(trendingKeywords);

            // 2) Prefetch Pytrends popularity for top-K trending candidates (limited external calls)
            PytrendsPrefetchResult pytrendsResult = prefetchPytrendsForTopKeywords(trendingKeywords);

            updateLastRunTimestamp();

            if (pytrendsResult.success()) {
                log.info("Successfully fetched/cached {} trending keywords and prefetched Pytrends", trendingKeywords.size());
            } else {
                log.warn("Fetched/cached {} trending keywords, but Pytrends prefetch failed: {}", trendingKeywords.size(), pytrendsResult.message());
            }

            return new TrendingPrefetchResult(
                    pytrendsResult.success() ? "completed" : "partial",
                    trendingKeywords.size(),
                    pytrendsResult.keywordCount(),
                    pytrendsResult.message()
            );

        } catch (Exception e) {
            log.error("Error during trending keywords fetch: {}", e.getMessage(), e);
            return new TrendingPrefetchResult("failed", 0, 0, safeMessage(e));
        }
    }

    /**
     * Daily job: compute Pytrends popularity for a limited set of keywords and store in Redis.
     * Target key format (must match HybridTrendService): trend:pyt:<keyword>
     */
    private PytrendsPrefetchResult prefetchPytrendsForTopKeywords(List<TrendingKeyword> trendingKeywords) {
        try {
            int batchSize = Math.min(Math.max(pytrendsBatchSize, 0), trendingKeywords.size());
            if (batchSize == 0) {
                return new PytrendsPrefetchResult(false, 0, "No Pytrends keywords selected");
            }

            List<String> topKeywords = trendingKeywords.stream()
                    .sorted((a, b) -> Double.compare(b.getTrendScore(), a.getTrendScore()))
                    .limit(batchSize)
                    .map(TrendingKeyword::getKeyword)
                    .collect(Collectors.toList());

            // Let Python read Redis connection via env vars, keep command simple.
            // Requires: backend/scripts/pytrends_prefetch.py exists.
            Path scriptPath = resolvePytrendsScriptPath();
            if (!Files.exists(scriptPath)) {
                String message = "Pytrends script not found at " + scriptPath.toAbsolutePath();
                log.warn(message);
                return new PytrendsPrefetchResult(false, batchSize, message);
            }

            // Build JSON args (python script expects argv[1] as JSON array)
            String jsonKeywords = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(topKeywords);

            ProcessBuilder pb = new ProcessBuilder(
                    pythonCommand,
                    scriptPath.toString()
            );

            // Forward Redis env (defaults match RedisConfig)
            pb.environment().put("REDIS_HOST", redisHost);
            pb.environment().put("REDIS_PORT", redisPort);
            pb.environment().put("REDIS_DB", redisDatabase);
            pb.environment().put("PYTRENDS_KEYWORDS", jsonKeywords);
            pb.environment().put("PYTRENDS_TTL_SECONDS", "86400"); // 24h
            pb.environment().put("PYTRENDS_SLEEP_SECONDS", pytrendsSleepSeconds);


            pb.redirectErrorStream(true);

            // Ensure JSON serialization won't fail due to Java time types.
            // (Handled by RedisConfig's ObjectMapper with JavaTimeModule)
            Process p = pb.start();

            List<String> output = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    output.add(line);
                    log.debug("pytrends_prefetch: {}", line);
                }
            }

            int exitCode = p.waitFor();
            String message = output.isEmpty()
                    ? "pytrends_prefetch produced no output"
                    : String.join(" | ", output);

            if (exitCode != 0) {
                log.warn("pytrends_prefetch exited with code {} for {} keywords: {}", exitCode, batchSize, message);
                return new PytrendsPrefetchResult(false, batchSize, message);
            }

            log.info("pytrends_prefetch exited with code {} for {} keywords: {}", exitCode, batchSize, message);
            return new PytrendsPrefetchResult(true, batchSize, message);
        } catch (Exception e) {
            log.warn("Skipping Pytrends prefetch due to error: {}", e.getMessage(), e);
            return new PytrendsPrefetchResult(false, 0, safeMessage(e));
        }
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private Path resolvePytrendsScriptPath() {
        List<Path> candidates = List.of(
                Paths.get(pytrendsScript),
                Paths.get("backend", "scripts", "pytrends_prefetch.py"),
                Paths.get("scripts", "pytrends_prefetch.py")
        );

        return candidates.stream()
                .filter(Files::exists)
                .findFirst()
                .orElse(candidates.get(0));
    }


    @Scheduled(fixedDelay = 3600000) // Every hour
    public void updateTrendingStats() {
        if (!schedulerEnabled) {
            return;
        }
        
        try {
            Map<String, Object> stats = generateTrendingStatistics();
            redisTemplate.opsForHash().putAll(TRENDING_STATS_KEY, stats);
            redisTemplate.expire(TRENDING_STATS_KEY, Duration.ofHours(25));
            
            log.debug("Updated trending statistics");
            
        } catch (Exception e) {
            log.error("Error updating trending statistics: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 1800000) // Every 30 minutes
    public void healthCheck() {
        if (!schedulerEnabled) {
            return;
        }
        
        try {
            boolean cacheHealthy = checkCacheHealth();
            boolean trendServiceHealthy = checkTrendServiceHealth();
            
            Map<String, Object> healthData = Map.of(
                    "cache_healthy", cacheHealthy,
                    "trend_service_healthy", trendServiceHealthy,
                    "timestamp", LocalDateTime.now().toString()
            );
            
            redisTemplate.opsForHash().putAll("trending:health", healthData);
            redisTemplate.expire("trending:health", Duration.ofMinutes(35));
            
            if (!cacheHealthy || !trendServiceHealthy) {
                log.warn("Health check failed - Cache: {}, Trend Service: {}", cacheHealthy, trendServiceHealthy);
            }
            
        } catch (Exception e) {
            log.error("Error during health check: {}", e.getMessage());
        }
    }

    private List<TrendingKeyword> fetchTrendingKeywords() {
        List<TrendingKeyword> trendingKeywords = new ArrayList<>();
        
        // Generate trending keywords from base keywords
        for (String baseKeyword : BASE_KEYWORDS) {
            List<TrendingKeyword> generatedKeywords = generateTrendingVariations(baseKeyword);
            trendingKeywords.addAll(generatedKeywords);
        }
        
        // Add some completely trending keywords
        trendingKeywords.addAll(generateHotTrendingKeywords());
        
        // Sort by trend score and limit
        trendingKeywords.sort((a, b) -> Double.compare(b.getTrendScore(), a.getTrendScore()));
        
        return trendingKeywords.stream()
                .limit(maxTrendingKeywords)
                .collect(Collectors.toList());
    }

    private List<TrendingKeyword> generateTrendingVariations(String baseKeyword) {
        List<TrendingKeyword> variations = new ArrayList<>();
        Random random = ThreadLocalRandom.current();
        
        // Get base trend data
        Optional<TrendService.TrendData> baseTrendData = trendService.getTrendData(baseKeyword);
        double baseScore = baseTrendData.map(TrendService.TrendData::popularityScore).orElse(50.0);
        long baseVolume = baseTrendData.map(TrendService.TrendData::searchVolume).orElse(10000L);
        
        // Generate variations with modifiers
        for (String modifier : TRENDING_MODIFIERS) {
            if (random.nextDouble() < 0.3) { // 30% chance for each modifier
                String variation = baseKeyword + " " + modifier;
                
                double variationScore = baseScore + (random.nextGaussian() * 10);
                variationScore = Math.max(0.0, Math.min(100.0, variationScore));
                
                long variationVolume = (long) (baseVolume * (0.5 + random.nextDouble()));
                
                double trendDirection = (random.nextGaussian() * 0.4);
                
                TrendingKeyword trendingKeyword = new TrendingKeyword(
                        variation,
                        variationScore,
                        variationVolume,
                        trendDirection,
                        LocalDateTime.now(),
                        "VARIATION"
                );
                
                variations.add(trendingKeyword);
            }
        }
        
        return variations;
    }

    private List<TrendingKeyword> generateHotTrendingKeywords() {
        List<TrendingKeyword> hotKeywords = new ArrayList<>();
        Random random = ThreadLocalRandom.current();
        
        List<String> hotTopics = Arrays.asList(
                "ai tools", "chatgpt", "machine learning", "web development", "cloud computing",
                "cybersecurity", "data science", "blockchain", "metaverse", "quantum computing",
                "sustainable tech", "green computing", "edge computing", "5g technology", "iot"
        );
        
        for (String topic : hotTopics) {
            double score = 80.0 + random.nextDouble() * 20.0;
            long volume = (long) (50000 + random.nextDouble() * 200000);
            double trendDirection = 0.1 + random.nextDouble() * 0.3;
            
            TrendingKeyword trendingKeyword = new TrendingKeyword(
                    topic,
                    score,
                    volume,
                    trendDirection,
                    LocalDateTime.now(),
                    "HOT_TREND"
            );
            
            hotKeywords.add(trendingKeyword);
        }
        
        return hotKeywords;
    }

    private void cacheTrendingKeywords(List<TrendingKeyword> trendingKeywords) {
        try {
            // Cache as KeywordResponseDTO objects
            List<KeywordResponseDTO> keywordDTOs = trendingKeywords.stream()
                    .map(this::convertToKeywordResponseDTO)
                    .collect(Collectors.toList());
            
            cacheService.cacheKeywords("trending:daily", keywordDTOs, cacheDuration);
            
            // Also cache raw trending data
            redisTemplate.opsForValue().set(TRENDING_CACHE_KEY, trendingKeywords, cacheDuration);
            
            // Cache individual keywords for quick lookup
            for (TrendingKeyword keyword : trendingKeywords) {
                String individualKey = "trending:keyword:" + keyword.getKeyword().replace(" ", ":");
                redisTemplate.opsForValue().set(individualKey, keyword, cacheDuration);
            }
            
            log.info("Cached {} trending keywords with TTL {}", trendingKeywords.size(), cacheDuration);
            
        } catch (Exception e) {
            log.error("Error caching trending keywords: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void updateTrendingStatistics(List<TrendingKeyword> trendingKeywords) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total_keywords", trendingKeywords.size());
        stats.put("avg_trend_score", trendingKeywords.stream()
                .mapToDouble(TrendingKeyword::getTrendScore)
                .average()
                .orElse(0.0));
        stats.put("avg_search_volume", trendingKeywords.stream()
                .mapToLong(TrendingKeyword::getSearchVolume)
                .average()
                .orElse(0.0));
        stats.put("trending_up_count", trendingKeywords.stream()
                .mapToLong(k -> k.getTrendDirection() > 0.1 ? 1 : 0)
                .sum());
        stats.put("hot_trends_count", trendingKeywords.stream()
                .mapToLong(k -> "HOT_TREND".equals(k.getSource()) ? 1 : 0)
                .sum());
        stats.put("variations_count", trendingKeywords.stream()
                .mapToLong(k -> "VARIATION".equals(k.getSource()) ? 1 : 0)
                .sum());
        stats.put("last_updated", LocalDateTime.now().toString());
        
        redisTemplate.opsForHash().putAll(TRENDING_STATS_KEY, stats);
        redisTemplate.expire(TRENDING_STATS_KEY, Duration.ofHours(25));
        
        log.debug("Updated trending statistics: {}", stats);
    }

    private void updateLastRunTimestamp() {
        redisTemplate.opsForValue().set(LAST_RUN_KEY, LocalDateTime.now().toString(), Duration.ofDays(7));
    }

    private Map<String, Object> generateTrendingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            Object cachedKeywords = redisTemplate.opsForValue().get(TRENDING_CACHE_KEY);
            if (cachedKeywords instanceof List) {
                @SuppressWarnings("unchecked")
                List<TrendingKeyword> keywords = (List<TrendingKeyword>) cachedKeywords;
                
                stats.put("cache_size", keywords.size());
                stats.put("cache_hit_rate", calculateCacheHitRate());
                stats.put("memory_usage", estimateMemoryUsage());
            }
        } catch (Exception e) {
            log.debug("Error generating statistics: {}", e.getMessage());
        }
        
        stats.put("timestamp", LocalDateTime.now().toString());
        return stats;
    }

    private boolean checkCacheHealth() {
        try {
            redisTemplate.opsForValue().get("health:check");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkTrendServiceHealth() {
        try {
            trendService.getTrendData("test");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private double calculateCacheHitRate() {
        // This would be implemented with actual cache metrics
        return 0.85 + (ThreadLocalRandom.current().nextDouble() * 0.1);
    }

    private long estimateMemoryUsage() {
        // Rough estimation in bytes
        return 1024 * 1024 + (ThreadLocalRandom.current().nextInt(500) * 1024);
    }

    private KeywordResponseDTO convertToKeywordResponseDTO(TrendingKeyword trendingKeyword) {
        String type = determineKeywordType(trendingKeyword.getKeyword());
        return new KeywordResponseDTO(
                trendingKeyword.getKeyword(),
                trendingKeyword.getTrendScore(),
                type,
                (int) trendingKeyword.getSearchVolume()
        );
    }

    private String determineKeywordType(String keyword) {
        int wordCount = keyword.split("\\s+").length;
        if (wordCount == 1) return "SHORT";
        if (wordCount == 2) return "MEDIUM";
        return "LONG_TAIL";
    }

    // Manual trigger method for testing/admin
    public TrendingPrefetchResult triggerTrendingKeywordsFetch() {
        log.info("Manual trigger of trending keywords fetch");
        return runTrendingKeywordsFetch();
    }

    public record TrendingPrefetchResult(String status, int cachedKeywords, int pytrendsKeywords, String message) {}

    private record PytrendsPrefetchResult(boolean success, int keywordCount, String message) {}

    public List<TrendingKeyword> getCachedTrendingKeywords() {
        try {
            Object cached = redisTemplate.opsForValue().get(TRENDING_CACHE_KEY);
            if (cached instanceof List<?> cachedList) {
                return cachedList.stream()
                        .map(this::toTrendingKeyword)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error retrieving cached trending keywords: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private TrendingKeyword toTrendingKeyword(Object value) {
        if (value instanceof TrendingKeyword keyword) {
            return keyword;
        }

        if (!(value instanceof Map<?, ?> rawMap)) {
            return null;
        }

        Map<String, Object> map = new HashMap<>();
        rawMap.forEach((key, mapValue) -> {
            if (key != null) {
                map.put(key.toString(), mapValue);
            }
        });

        String keyword = asString(map.get("keyword"));
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return new TrendingKeyword(
                keyword,
                asDouble(map.get("trendScore")),
                asLong(map.get("searchVolume")),
                asDouble(map.get("trendDirection")),
                asLocalDateTime(map.get("timestamp")),
                Optional.ofNullable(asString(map.get("source"))).orElse("CACHE")
        );
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private LocalDateTime asLocalDateTime(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof String text) {
            try {
                return LocalDateTime.parse(text);
            } catch (Exception ignored) {
                return LocalDateTime.now();
            }
        }
        return LocalDateTime.now();
    }

    // Get trending statistics
    public Map<String, Object> getTrendingStatistics() {
        try {
            Map<Object, Object> rawStats = redisTemplate.opsForHash().entries(TRENDING_STATS_KEY);
            Map<String, Object> stats = new HashMap<>();
            rawStats.forEach((key, value) -> stats.put(key.toString(), value));
            return stats;
        } catch (Exception e) {
            log.error("Error retrieving trending statistics: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    public static class TrendingKeyword {
        private final String keyword;
        private final double trendScore;
        private final long searchVolume;
        private final double trendDirection;
        private final LocalDateTime timestamp;
        private final String source;

        public TrendingKeyword(String keyword, double trendScore, long searchVolume,
                              double trendDirection, LocalDateTime timestamp, String source) {
            this.keyword = keyword;
            this.trendScore = trendScore;
            this.searchVolume = searchVolume;
            this.trendDirection = trendDirection;
            this.timestamp = timestamp;
            this.source = source;
        }

        public String getKeyword() { return keyword; }
        public double getTrendScore() { return trendScore; }
        public long getSearchVolume() { return searchVolume; }
        public double getTrendDirection() { return trendDirection; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getSource() { return source; }

        public boolean isTrendingUp() {
            return trendDirection > 0.1;
        }

        public boolean isHotTrend() {
            return "HOT_TREND".equals(source);
        }

        @Override
        public String toString() {
            return String.format("%s [Score: %.1f, Volume: %d, Trend: %.2f, Source: %s]",
                    keyword, trendScore, searchVolume, trendDirection, source);
        }
    }
}
