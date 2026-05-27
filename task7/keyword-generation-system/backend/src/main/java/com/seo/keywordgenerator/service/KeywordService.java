package com.seo.keywordgenerator.service;

import com.seo.keywordgenerator.dto.KeywordRequestDTO;
import com.seo.keywordgenerator.dto.KeywordResponseDTO;
import com.seo.keywordgenerator.model.Keyword;
import com.seo.keywordgenerator.repository.KeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class KeywordService {

    private final KeywordRepository keywordRepository;
    private final KeywordGenerationPipeline keywordGenerationPipeline;
    private final CacheService cacheService;

    public List<KeywordResponseDTO> generateKeywords(KeywordRequestDTO request) {
        log.info("Starting keyword generation for content: {}", request.getContent());
        
        try {
            // Use the proper pipeline architecture
            KeywordGenerationPipeline.PipelineResult pipelineResult = keywordGenerationPipeline.generateKeywords(request);
            
            if (pipelineResult.isSuccess()) {
                List<KeywordResponseDTO> keywords = pipelineResult.getKeywords();
                
                // Store keywords in database (only for new content, not cache hits)
                if (!pipelineResult.isCacheHit() && !pipelineResult.getStatus().equals("DUPLICATE_CONTENT_CACHE_HIT")) {
                    String contentId = generateContentId(request.getContent());
                    for (KeywordResponseDTO keyword : keywords) {
                        Keyword keywordEntity = new Keyword(
                                keyword.getKeyword(),
                                keyword.getScore(),
                                keyword.getType(),
                                contentId,
                                keyword.getSearchVolume(),
                                keyword.isEstimated(),
                                keyword.getSource(),
                                keyword.getValidationStatus(),
                                keyword.getTrendDirection(),
                                keyword.getValidatedAt()
                        );
                        keywordRepository.save(keywordEntity);
                    }
                    log.info("Successfully generated and stored {} keywords", keywords.size());
                } else {
                    log.info("Returning cached results, no database storage needed for {} keywords", keywords.size());
                }
                return keywords;
            } else {
                // Handle cache hits as success cases
                if (pipelineResult.getStatus().equals("DUPLICATE_CONTENT_CACHE_HIT")) {
                    log.info("Cache hit successful for {} keywords", pipelineResult.getKeywords().size());
                    return pipelineResult.getKeywords();
                }
                log.error("Pipeline failed with status: {}", pipelineResult.getStatus());
                throw new RuntimeException("Keyword generation pipeline failed: " + pipelineResult.getStatus());
            }
                    
        } catch (Exception e) {
            log.error("Error in keyword generation: {}", e.getMessage(), e);
            throw new RuntimeException("Keyword generation failed", e);
        }
    }

    public void clearCache() {
        log.info("Clearing all keyword-related caches");
        try {
            cacheService.clearAllCache();
            keywordGenerationPipeline.resetMetrics();
            log.info("All caches cleared successfully");
        } catch (Exception e) {
            log.error("Error clearing caches: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to clear caches", e);
        }
    }

    @Cacheable(value = "keywords", key = "#contentId")
    public List<KeywordResponseDTO> getKeywordsByContentId(String contentId) {
        List<Keyword> keywords = keywordRepository.findByContentIdOrderByScoreDesc(contentId);
        return keywords.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<KeywordResponseDTO> searchKeywords(String term) {
        List<Keyword> keywords = keywordRepository.findByKeywordContainingIgnoreCaseOrderByScoreDesc(term);
        return keywords.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public KeywordGenerationPipeline.PipelineMetrics getPipelineMetrics() {
        return keywordGenerationPipeline.getMetrics();
    }

    public KeywordGenerationPipeline.HealthStatus getHealthStatus() {
        return keywordGenerationPipeline.getHealthStatus();
    }

    public void resetPipelineMetrics() {
        keywordGenerationPipeline.resetMetrics();
    }
    
    /**
     * Async keyword generation for non-critical tasks
     * Returns CompletableFuture for async processing
     */
    @Async
    public CompletableFuture<List<KeywordResponseDTO>> generateKeywordsAsync(KeywordRequestDTO request) {
        try {
            List<KeywordResponseDTO> keywords = generateKeywords(request);
            return CompletableFuture.completedFuture(keywords);
        } catch (Exception e) {
            log.error("Async keyword generation failed: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Async database storage - non-critical background task
     */
    @Async
    public CompletableFuture<Void> storeKeywordsAsync(List<KeywordResponseDTO> keywords, KeywordRequestDTO request) {
        try {
            storeResultsInDatabase(keywords, request);
            log.debug("Async storage completed for {} keywords", keywords.size());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Async keyword storage failed: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Batch processing for multiple requests
     */
    @Async
    public CompletableFuture<Map<String, List<KeywordResponseDTO>>> batchGenerateKeywords(List<KeywordRequestDTO> requests) {
        Map<String, List<KeywordResponseDTO>> results = new HashMap<>();
        
        try {
            // Use Set for uniqueness and avoid duplicate processing
            Set<String> processedContents = new HashSet<>();
            
            for (KeywordRequestDTO request : requests) {
                String content = request.getContent();
                
                // Skip if already processed (duplicate avoidance)
                if (!processedContents.add(content)) {
                    log.debug("Skipping duplicate content in batch: {}", content.substring(0, Math.min(20, content.length())));
                    continue;
                }
                
                try {
                    List<KeywordResponseDTO> keywords = generateKeywords(request);
                    results.put(content, keywords);
                } catch (Exception e) {
                    log.error("Failed to process content in batch: {}", e.getMessage());
                    results.put(content, Collections.emptyList());
                }
            }
            
            log.info("Batch processing completed: {} requests processed", results.size());
            return CompletableFuture.completedFuture(results);
            
        } catch (Exception e) {
            log.error("Batch keyword generation failed: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    private void storeResultsInDatabase(List<KeywordResponseDTO> keywords, KeywordRequestDTO request) {
        String contentId = generateContentId(request.getContent());
        
        for (KeywordResponseDTO keywordDTO : keywords) {
            Keyword keywordEntity = new Keyword(
                    keywordDTO.getKeyword(),
                    keywordDTO.getScore(),
                    keywordDTO.getType(),
                    contentId,
                    keywordDTO.getSearchVolume(),
                    keywordDTO.isEstimated(),
                    keywordDTO.getSource(),
                    keywordDTO.getValidationStatus(),
                    keywordDTO.getTrendDirection(),
                    keywordDTO.getValidatedAt()
            );
            keywordRepository.save(keywordEntity);
        }
        
        log.debug("Stored {} keywords in database for contentId: {}", keywords.size(), contentId);
    }

    private List<KeywordResponseDTO> fallbackKeywordGeneration(KeywordRequestDTO request) {
        log.warn("Using fallback keyword generation due to pipeline failure");
        
        // Basic fallback implementation
        List<KeywordResponseDTO> fallbackKeywords = new ArrayList<>();
        String[] words = request.getContent().toLowerCase().split("\\s+");
        
        Set<String> uniqueWords = new HashSet<>();
        for (String word : words) {
            if (word.length() >= 3 && uniqueWords.add(word)) {
                double score = Math.random() * 50 + 50; // Random score between 50-100
                String type = word.split("\\s+").length == 1 ? "SHORT" : "MEDIUM";
                
                fallbackKeywords.add(new KeywordResponseDTO(word, score, type));
                
                if (fallbackKeywords.size() >= request.getMaxKeywords()) {
                    break;
                }
            }
        }
        
        return fallbackKeywords.stream()
                .sorted(Comparator.comparingDouble(KeywordResponseDTO::getScore).reversed())
                .limit(request.getMaxKeywords())
                .collect(Collectors.toList());
    }

    private String generateContentId(String content) {
        return "content_" + Math.abs(content.hashCode());
    }

    private KeywordResponseDTO convertToDTO(Keyword keyword) {
        KeywordResponseDTO dto = new KeywordResponseDTO(
                keyword.getKeyword(),
                keyword.getScore(),
                keyword.getType(),
                keyword.getSearchVolume() != null ? keyword.getSearchVolume().intValue() : 0
        );
        dto.setEstimated(keyword.isEstimated());
        dto.setSource(keyword.getSource());
        dto.setValidationStatus(keyword.getValidationStatus());
        dto.setTrendDirection(keyword.getTrendDirection() != null ? keyword.getTrendDirection() : 0.0);
        dto.setValidatedAt(keyword.getValidatedAt());
        dto.setConfidenceScore(Math.round(Math.min(100.0, Math.max(0.0, keyword.getScore()))));
        dto.setSearchIntent(inferSearchIntent(keyword.getKeyword()));
        dto.setPopularityTier(keyword.getScore() >= 80 ? "HIGH" : keyword.getScore() >= 60 ? "MEDIUM" : "LOW");
        dto.setCluster(inferCluster(keyword.getKeyword(), dto.getSearchIntent()));
        return dto;
    }

    private String inferSearchIntent(String keyword) {
        String lower = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        if (lower.contains(" vs ") || lower.startsWith("best ") && lower.contains(" for ")) return "COMPARISON";
        if (lower.contains("beginner")) return "BEGINNER_FOCUSED";
        if (lower.contains("tutorial") || lower.startsWith("how to use") || lower.contains("setup")) return "TUTORIAL";
        if (lower.contains("optimization") || lower.contains("best practices") || lower.contains("performance")) return "OPTIMIZATION_FOCUSED";
        return "INFORMATIONAL";
    }

    private String inferCluster(String keyword, String intent) {
        String lower = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        String topic = Arrays.stream(lower.split("\\s+"))
                .filter(word -> !Set.of("how", "to", "learn", "use", "best", "tutorial", "guide", "beginner", "beginners",
                        "explained", "setup", "optimization", "practices", "for", "with", "vs").contains(word))
                .limit(2)
                .map(word -> word.isBlank() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
        if (topic.isBlank()) topic = "General";
        if ("COMPARISON".equals(intent)) return topic + " Comparison";
        if ("OPTIMIZATION_FOCUSED".equals(intent)) return topic + " Optimization";
        if ("TUTORIAL".equals(intent) || "BEGINNER_FOCUSED".equals(intent)) return topic + " Learning";
        return topic + " Research";
    }
}
