package com.seo.keywordgenerator.service;

import com.seo.keywordgenerator.dto.KeywordRequestDTO;
import com.seo.keywordgenerator.dto.KeywordResponseDTO;
import com.seo.keywordgenerator.util.KeywordGeneratorEngineNew;
import com.seo.keywordgenerator.dto.KeywordValidationResult;
import com.seo.keywordgenerator.scheduler.TrendingKeywordScheduler;
import com.seo.keywordgenerator.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeywordGenerationPipeline {

    private final TextPreprocessor textPreprocessor;
    private final KeywordExtractor keywordExtractor;
    private final RAKEExtractor rakeExtractor;
    private final KeywordGeneratorEngineNew keywordGeneratorEngineNew;
    private final SEOOptimizer seoOptimizer;
    private final KeywordValidator keywordValidator;
    private final CacheService cacheService;
    private final TrendingKeywordScheduler trendingScheduler;
    private final KeywordExtractionService keywordExtractionService;
    private final KeywordOptimizationService keywordOptimizationService;
    private final KeywordRankingService keywordRankingService;
    private final KeywordValidationService keywordValidationService;
    private final KeywordClusterService keywordClusterService;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    
    private long totalProcessed = 0;
    private long cacheHits = 0;
    private long cacheMisses = 0;
    private long totalProcessingTime = 0;

    public PipelineResult generateKeywords(KeywordRequestDTO request) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("Starting optimized keyword generation pipeline for content length: {}", request.getContent().length());
            
            // Step 1: Check if content has been processed before (duplicate avoidance)
            if (cacheService.isContentProcessed(request.getContent())) {
                log.info("Content already processed, checking cache");
                List<KeywordResponseDTO> cachedResult = cacheService.getCachedKeywords(request.getContent());
                if (cachedResult != null) {
                    cacheHits++;
                    long processingTime = System.currentTimeMillis() - startTime;
                    cachedResult.forEach(keyword -> keyword.setProcessingTimeMs(processingTime));
                    log.info("Duplicate content detected! Returned {} cached keywords in {}ms", cachedResult.size(), processingTime);
                    return new PipelineResult(cachedResult, "DUPLICATE_CONTENT_CACHE_HIT", processingTime);
                }
            }
            
            // Step 2: Check cache first
            List<KeywordResponseDTO> cachedResult = cacheService.getCachedKeywords(request.getContent());
            if (cachedResult != null) {
                cacheHits++;
                long processingTime = System.currentTimeMillis() - startTime;
                cachedResult.forEach(keyword -> keyword.setProcessingTimeMs(processingTime));
                log.info("Cache hit! Returned {} cached keywords in {}ms", cachedResult.size(), processingTime);
                return new PipelineResult(cachedResult, "CACHE_HIT", processingTime);
            }
            
            cacheMisses++;
            log.debug("Cache miss, proceeding with optimized pipeline processing");
            
            KeywordExtractionService.ExtractionResult extraction = keywordExtractionService.extract(request.getContent());
            log.debug("Extracted {} raw keywords and {} topics", extraction.rawKeywords().size(), extraction.topics().size());

            List<KeywordOptimizationService.KeywordCandidate> candidates = keywordOptimizationService.generateCandidates(
                    extraction, request.getContent(), request.getMaxKeywords() * 4);
            log.debug("Generated {} natural keyword candidates", candidates.size());

            List<KeywordRankingService.RankedKeyword> rankedKeywords = keywordRankingService.rank(
                    candidates, extraction, request.getMaxKeywords());
            log.debug("Ranked {} keyword candidates", rankedKeywords.size());

            List<KeywordResponseDTO> validatedKeywords = keywordValidationService.validateAndEnrich(rankedKeywords, extraction);
            log.debug("Validated and enriched {} keywords", validatedKeywords.size());

            List<KeywordResponseDTO> finalKeywords = applyFinalFilteringAndSortingOptimized(validatedKeywords, request.getMaxKeywords());
            keywordClusterService.assignClusters(finalKeywords);
            log.debug("Final filtered {} keywords", finalKeywords.size());
            
            // Step 9: Mark content as processed and cache results
            cacheService.markContentAsProcessed(request.getContent());
            cacheService.cacheKeywordsAsJson(request.getContent(), finalKeywords, Duration.ofHours(24));
            log.debug("Marked content as processed and cached {} keywords", finalKeywords.size());
            
            long processingTime = System.currentTimeMillis() - startTime;
            finalKeywords.forEach(keyword -> keyword.setProcessingTimeMs(processingTime));
            totalProcessed++;
            totalProcessingTime += processingTime;
            
            log.info("Optimized pipeline completed: {} keywords generated in {}ms", finalKeywords.size(), processingTime);
            return new PipelineResult(finalKeywords, "SUCCESS", processingTime);
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Optimized pipeline failed after {}ms: {}", processingTime, e.getMessage(), e);
            return new PipelineResult(Collections.emptyList(), "FAILED: " + e.getMessage(), processingTime);
        }
    }
    
    /**
     * Optimized parallel extraction using Sets for uniqueness (avoids nested loops)
     */
    private Set<String> extractKeywordsParallelOptimized(String content) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Use CompletableFuture for parallel extraction with error handling
            CompletableFuture<List<String>> keywordExtractorFuture = 
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return keywordExtractor.extractKeywords(content);
                    } catch (Exception e) {
                        log.warn("Error in keyword extractor: {}", e.getMessage());
                        return Collections.emptyList();
                    }
                }, executorService);
            
                        
            CompletableFuture<List<String>> rakeExtractorFuture = 
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return rakeExtractor.extractKeywords(content).stream()
                                .map(RAKEExtractor.RAKEScore::getPhrase)
                                .collect(Collectors.toList());
                    } catch (Exception e) {
                        log.warn("Error in RAKE extractor: {}", e.getMessage());
                        return Collections.emptyList();
                    }
                }, executorService);
            
            Set<String> contentTokens = extractContentTokens(content);

            CompletableFuture<List<String>> trendingFuture =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        List<TrendingKeywordScheduler.TrendingKeyword> trendingKeywords = trendingScheduler.getCachedTrendingKeywords();
                        if (trendingKeywords != null) {
                            return trendingKeywords.stream()
                                    .map(TrendingKeywordScheduler.TrendingKeyword::getKeyword)
                                    .filter(keyword -> isTrendingKeywordRelevant(keyword, contentTokens))
                                    .collect(Collectors.toList());
                        }
                        return Collections.emptyList();
                    } catch (Exception e) {
                        log.warn("Error getting trending keywords: {}", e.getMessage());
                        return Collections.emptyList();
                    }
                }, executorService);
            
            // Wait for all to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                keywordExtractorFuture, rakeExtractorFuture, trendingFuture);
            
            allFutures.join();
            
            // Use Set for automatic uniqueness (more efficient than distinct() stream)
            Set<String> uniqueKeywords = new HashSet<>();
            
            try {
                uniqueKeywords.addAll(keywordExtractorFuture.get());
                uniqueKeywords.addAll(rakeExtractorFuture.get());
                uniqueKeywords.addAll(trendingFuture.get());
            } catch (Exception e) {
                log.error("Error collecting optimized parallel extraction results: {}", e.getMessage());
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.debug("Optimized parallel keyword extraction completed in {}ms: {} unique keywords extracted", 
                processingTime, uniqueKeywords.size());
            
            return uniqueKeywords;
            
        } catch (Exception e) {
            log.error("Optimized parallel keyword extraction failed: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    private Set<String> extractContentTokens(String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptySet();
        }

        return Arrays.stream(content.toLowerCase(Locale.ROOT).split("\\s+"))
                .map(token -> token.replaceAll("[^a-z0-9]", ""))
                .filter(token -> token.length() >= 3)
                .collect(Collectors.toSet());
    }

    private boolean isTrendingKeywordRelevant(String keyword, Set<String> contentTokens) {
        if (keyword == null || contentTokens == null || contentTokens.isEmpty()) {
            return false;
        }

        long overlap = Arrays.stream(keyword.toLowerCase(Locale.ROOT).split("\\s+"))
                .map(token -> token.replaceAll("[^a-z0-9]", ""))
                .filter(token -> token.length() >= 3)
                .filter(contentTokens::contains)
                .count();

        return overlap > 0;
    }
    
    /**
     * Batch validation for better performance
     */
    private List<KeywordResponseDTO> validateAndScoreKeywordsBatch(List<KeywordResponseDTO> keywords) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Convert to list of keyword strings for batch processing
            List<String> keywordStrings = keywords.stream()
                    .map(KeywordResponseDTO::getKeyword)
                    .collect(Collectors.toList());
            
            // Batch validate
            List<KeywordValidationResult> validationResults = keywordValidator.validateKeywordsWithBatchProcessing(keywordStrings);
            Map<String, KeywordValidationResult> validationByKeyword = validationResults.stream()
                    .collect(Collectors.toMap(
                            result -> result.getKeyword().toLowerCase(Locale.ROOT).trim(),
                            result -> result,
                            (first, ignored) -> first
                    ));
            
            // Convert back to DTOs
            List<KeywordResponseDTO> validatedKeywords = new ArrayList<>();
            
            for (int i = 0; i < keywords.size(); i++) {
                KeywordResponseDTO original = keywords.get(i);
                KeywordValidationResult validation = validationByKeyword.get(
                        original.getKeyword().toLowerCase(Locale.ROOT).trim());

                if (validation == null) {
                    continue;
                }
                
                if (validation.isValid()) {
                    KeywordResponseDTO validated = new KeywordResponseDTO(
                            original.getKeyword(),
                            original.getScore(), // Preserve original score from HybridTrendService
                            original.getType()
                    );
                    validated.setSearchVolume((int) validation.getSearchVolume());
                    validated.setEstimated(validation.isEstimated());
                    validated.setSource(validation.getSource());
                    validated.setValidationStatus(validation.getValidationStatus().toString());
                    validated.setTrendDirection(validation.getTrendDirection());
                    validated.setValidatedAt(validation.getValidatedAt());
                    validatedKeywords.add(validated);
                }
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.debug("Batch validation completed in {}ms: {} keywords validated", 
                processingTime, validatedKeywords.size());
            
            return validatedKeywords;
            
        } catch (Exception e) {
            log.error("Batch validation failed: {}", e.getMessage(), e);
            return keywords; // Return original keywords on validation failure
        }
    }
    
    private String determineKeywordType(String keyword) {
        int wordCount = keyword.split("\\s+").length;
        if (wordCount == 1) return "SHORT";
        if (wordCount == 2) return "MEDIUM";
        return "LONG_TAIL";
    }
    
    /**
     * Apply final filtering and sorting using optimized data structures
     */
    private List<KeywordResponseDTO> applyFinalFilteringAndSortingOptimized(List<KeywordResponseDTO> keywords, int maxKeywords) {
        if (keywords == null || keywords.isEmpty()) {
            return Collections.emptyList();
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Use Set for uniqueness and remove duplicates
            Set<String> seenKeywords = new HashSet<>();
            List<KeywordResponseDTO> uniqueKeywords = new ArrayList<>();
            
            for (KeywordResponseDTO keyword : keywords) {
                if (keyword != null && keyword.getKeyword() != null && 
                    !seenKeywords.contains(keyword.getKeyword().toLowerCase(Locale.ROOT).trim())) {
                    seenKeywords.add(keyword.getKeyword().toLowerCase(Locale.ROOT).trim());
                    uniqueKeywords.add(keyword);
                }
            }
            
            // Sort by score (descending) and limit to maxKeywords
            List<KeywordResponseDTO> sortedKeywords = uniqueKeywords.stream()
                    .sorted(Comparator
                            .comparing(KeywordResponseDTO::isEstimated)
                            .thenComparing(KeywordResponseDTO::getScore, Comparator.reverseOrder()))
                    .limit(maxKeywords)
                    .collect(Collectors.toList());
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.debug("Final filtering completed in {}ms: {} unique keywords", processingTime, sortedKeywords.size());
            
            return sortedKeywords;
            
        } catch (Exception e) {
            log.error("Error in final filtering and sorting: {}", e.getMessage(), e);
            return keywords.stream()
                    .limit(maxKeywords)
                    .collect(Collectors.toList());
        }
    }
    
    /**
     * Get pipeline metrics
     */
    public PipelineMetrics getMetrics() {
        double avgProcessingTime = totalProcessed > 0 ? (double) totalProcessingTime / totalProcessed : 0.0;
        double cacheHitRate = (cacheHits + cacheMisses) > 0 ? (double) cacheHits / (cacheHits + cacheMisses) * 100 : 0.0;
        
        return new PipelineMetrics(
                totalProcessed,
                cacheHits,
                cacheMisses,
                cacheHitRate,
                (long) avgProcessingTime
        );
    }
    
    /**
     * Get health status
     */
    public HealthStatus getHealthStatus() {
        Map<String, Object> components = new HashMap<>();
        components.put("cache", cacheHits > 0 ? "healthy" : "warning");
        components.put("processing", totalProcessed > 0 ? "healthy" : "idle");
        components.put("totalProcessed", totalProcessed);
        components.put("cacheHitRate", (cacheHits + cacheMisses) > 0 ? (double) cacheHits / (cacheHits + cacheMisses) * 100 : 0.0);
        
        String status = "healthy"; // Default status
        if (cacheHits == 0 && totalProcessed > 10) {
            status = "warning"; // Low cache hit rate
        }
        
        return new HealthStatus(status, components);
    }
    
    /**
     * Reset pipeline metrics
     */
    public void resetMetrics() {
        totalProcessed = 0;
        cacheHits = 0;
        cacheMisses = 0;
        totalProcessingTime = 0;
        log.info("Pipeline metrics reset");
    }
    
    public static class PipelineResult {
        private final List<KeywordResponseDTO> keywords;
        private final String status;
        private final long processingTimeMs;
        private final LocalDateTime timestamp;

        public PipelineResult(List<KeywordResponseDTO> keywords, String status, long processingTimeMs) {
            this.keywords = keywords;
            this.status = status;
            this.processingTimeMs = processingTimeMs;
            this.timestamp = LocalDateTime.now();
        }

        public List<KeywordResponseDTO> getKeywords() { return keywords; }
        public String getStatus() { return status; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        public boolean isSuccess() { return "SUCCESS".equals(status) || "CACHE_HIT".equals(status); }
        public boolean isCacheHit() { return "CACHE_HIT".equals(status); }
        
        @Override
        public String toString() {
            return String.format("PipelineResult{status=%s, keywords=%d, time=%dms, cache=%s}",
                    status, keywords.size(), processingTimeMs, isCacheHit());
        }
    }
    
    public static class PipelineMetrics {
        private final long totalProcessed;
        private final long cacheHits;
        private final long cacheMisses;
        private final double cacheHitRate;
        private final long avgProcessingTime;
        private final LocalDateTime timestamp;

        public PipelineMetrics(long totalProcessed, long cacheHits, long cacheMisses, 
                              double cacheHitRate, long avgProcessingTime) {
            this.totalProcessed = totalProcessed;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.cacheHitRate = cacheHitRate;
            this.avgProcessingTime = avgProcessingTime;
            this.timestamp = LocalDateTime.now();
        }

        public long getTotalProcessed() { return totalProcessed; }
        public long getCacheHits() { return cacheHits; }
        public long getCacheMisses() { return cacheMisses; }
        public double getCacheHitRate() { return cacheHitRate; }
        public long getAvgProcessingTime() { return avgProcessingTime; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("PipelineMetrics{processed=%d, hits=%d, misses=%d, hitRate=%.2f%%, avgTime=%dms}",
                    totalProcessed, cacheHits, cacheMisses, cacheHitRate, avgProcessingTime);
        }
    }
    
    public static class HealthStatus {
        private final String status;
        private final Map<String, Object> components;
        private final LocalDateTime timestamp;

        public HealthStatus(String status, Map<String, Object> components) {
            this.status = status;
            this.components = components;
            this.timestamp = LocalDateTime.now();
        }

        public String getStatus() { return status; }
        public Map<String, Object> getComponents() { return components; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("HealthStatus{status=%s, components=%d, time=%s}",
                    status, components.size(), timestamp);
        }
    }
}
