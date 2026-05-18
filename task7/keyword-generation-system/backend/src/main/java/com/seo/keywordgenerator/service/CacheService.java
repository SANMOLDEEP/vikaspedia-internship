package com.seo.keywordgenerator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seo.keywordgenerator.dto.KeywordResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String CACHE_PREFIX = "keyword:";
    private static final String STATS_PREFIX = "stats:";
    private static final String CONTENT_HASH_PREFIX = "content_hash:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24); // 24 hours TTL as requested
    private static final Duration LONG_TTL = Duration.ofHours(24);
    private static final Duration SHORT_TTL = Duration.ofMinutes(10);
    
    private long cacheHits = 0;
    private long cacheMisses = 0;
    private long cacheStores = 0;

    public List<KeywordResponseDTO> getCachedKeywords(String content) {
        return getCachedKeywords(content, DEFAULT_TTL);
    }
    
    public List<KeywordResponseDTO> getCachedKeywords(String content, Duration ttl) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        
        String contentHash = generateContentHash(content);
        String cacheKey = CACHE_PREFIX + contentHash;
        
        try {
            Object cachedObject = redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedObject != null) {
                cacheHits++;
                log.debug("Cache hit for content hash: {}", contentHash);
                return objectMapper.readValue(cachedObject.toString(), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, KeywordResponseDTO.class));
            } else {
                cacheMisses++;
                log.debug("Cache MISS for key: {}", cacheKey);
            }
        } catch (Exception e) {
            log.error("Error retrieving from cache for key {}: {}", cacheKey, e.getMessage());
            cacheMisses++;
        }
        
        return null;
    }
    
    public void cacheKeywords(String content, List<KeywordResponseDTO> keywords) {
        cacheKeywords(content, keywords, DEFAULT_TTL);
    }
    
    public void cacheKeywords(String content, List<KeywordResponseDTO> keywords, Duration ttl) {
        if (content == null || content.trim().isEmpty() || keywords == null || keywords.isEmpty()) {
            log.debug("Skipping cache - invalid content or keywords");
            return;
        }
        
        String contentHash = generateContentHash(content);
        String cacheKey = CACHE_PREFIX + contentHash;
        
        try {
            redisTemplate.opsForValue().set(cacheKey, keywords, ttl);
            cacheStores++;
            log.info("Cached {} keywords for content hash {} with TTL {}", keywords.size(), contentHash, ttl);
            
            updateCacheStatistics();
            
        } catch (Exception e) {
            log.error("Error caching keywords for key {}: {}", cacheKey, e.getMessage());
        }
    }
    
    public void cacheKeywordsAsJson(String content, List<KeywordResponseDTO> keywords, Duration ttl) {
        if (content == null || content.trim().isEmpty() || keywords == null || keywords.isEmpty()) {
            return;
        }
        
        String cacheKey = generateCacheKey(content);
        
        try {
            String jsonValue = serializeKeywords(keywords);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, ttl);
            cacheStores++;
            log.info("Cached {} keywords as JSON for key {} with TTL {}", keywords.size(), cacheKey, ttl);
            
        } catch (Exception e) {
            log.error("Error caching keywords as JSON for key {}: {}", cacheKey, e.getMessage());
        }
    }
    
    public boolean invalidateCache(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        String cacheKey = generateCacheKey(content);
        
        try {
            Boolean result = redisTemplate.delete(cacheKey);
            if (Boolean.TRUE.equals(result)) {
                log.info("Invalidated cache for key: {}", cacheKey);
                return true;
            } else {
                log.debug("No cache entry found to invalidate for key: {}", cacheKey);
                return false;
            }
        } catch (Exception e) {
            log.error("Error invalidating cache for key {}: {}", cacheKey, e.getMessage());
            return false;
        }
    }
    
    public void clearAllCache() {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} cache entries", keys.size());
            }
        } catch (Exception e) {
            log.error("Error clearing all cache: {}", e.getMessage());
        }
    }
    
    public boolean existsInCache(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        String cacheKey = generateCacheKey(content);
        
        try {
            Boolean exists = redisTemplate.hasKey(cacheKey);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Error checking cache existence for key {}: {}", cacheKey, e.getMessage());
            return false;
        }
    }
    
    public Duration getCacheTTL(String content) {
        if (content == null || content.trim().isEmpty()) {
            return Duration.ZERO;
        }
        
        String cacheKey = generateCacheKey(content);
        
        try {
            Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
            if (ttl != null && ttl > 0) {
                return Duration.ofSeconds(ttl);
            }
        } catch (Exception e) {
            log.error("Error getting TTL for key {}: {}", cacheKey, e.getMessage());
        }
        
        return Duration.ZERO;
    }
    
    public CacheStatistics getCacheStatistics() {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
            int totalKeys = keys != null ? keys.size() : 0;
            
            return new CacheStatistics(
                    cacheHits,
                    cacheMisses,
                    cacheStores,
                    totalKeys,
                    calculateHitRate(),
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            log.error("Error getting cache statistics: {}", e.getMessage());
            return new CacheStatistics(
                    cacheHits,
                    cacheMisses,
                    cacheStores,
                    0,
                    calculateHitRate(),
                    LocalDateTime.now()
            );
        }
    }
    
    public void resetStatistics() {
        cacheHits = 0;
        cacheMisses = 0;
        cacheStores = 0;
        log.info("Cache statistics reset");
    }
    
    private String generateCacheKey(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return CACHE_PREFIX + hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating cache key: {}", e.getMessage());
            return CACHE_PREFIX + content.hashCode();
        }
    }
    
    private String serializeKeywords(List<KeywordResponseDTO> keywords) throws JsonProcessingException {
        return objectMapper.writeValueAsString(keywords);
    }
    
    private List<KeywordResponseDTO> deserializeKeywords(String jsonValue) {
        try {
            return objectMapper.readValue(jsonValue, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, KeywordResponseDTO.class));
        } catch (JsonProcessingException e) {
            log.error("Error deserializing keywords from JSON: {}", e.getMessage());
            return null;
        }
    }
    
    private void updateCacheStatistics() {
        try {
            String statsKey = STATS_PREFIX + "daily";
            redisTemplate.opsForValue().increment(statsKey);
            redisTemplate.expire(statsKey, Duration.ofDays(1));
        } catch (Exception e) {
            log.debug("Error updating cache statistics: {}", e.getMessage());
        }
    }
    
    private double calculateHitRate() {
        long totalRequests = cacheHits + cacheMisses;
        return totalRequests > 0 ? (double) cacheHits / totalRequests * 100 : 0.0;
    }
    
    public Duration getRecommendedTTL(int keywordCount) {
        if (keywordCount > 50) {
            return LONG_TTL;
        } else if (keywordCount > 20) {
            return DEFAULT_TTL;
        } else {
            return SHORT_TTL;
        }
    }
    
    public void cacheWithAdaptiveTTL(String content, List<KeywordResponseDTO> keywords) {
        Duration ttl = getRecommendedTTL(keywords.size());
        cacheKeywords(content, keywords, ttl);
    }
    
    /**
     * Generate content hash for cache key and duplicate avoidance
     * Uses SHA-256 for consistent hashing
     */
    public String generateContentHash(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.trim().getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating content hash: {}", e.getMessage());
            // Fallback to simple hash
            return String.valueOf(content.trim().hashCode());
        }
    }
    
    /**
     * Check if content has been processed before (duplicate avoidance)
     */
    public boolean isContentProcessed(String content) {
        String contentHash = generateContentHash(content);
        String processedKey = CONTENT_HASH_PREFIX + contentHash;
        
        try {
            Boolean exists = redisTemplate.hasKey(processedKey);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Error checking if content processed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Mark content as processed to avoid duplicate processing
     */
    public void markContentAsProcessed(String content) {
        String contentHash = generateContentHash(content);
        String processedKey = CONTENT_HASH_PREFIX + contentHash;
        
        try {
            redisTemplate.opsForValue().set(processedKey, System.currentTimeMillis(), DEFAULT_TTL);
            log.debug("Marked content as processed: {}", contentHash.substring(0, 8));
        } catch (Exception e) {
            log.error("Error marking content as processed: {}", e.getMessage());
        }
    }
    
    /**
     * Batch cache multiple contents efficiently using Redis pipeline
     * Provides significant performance improvement for bulk operations
     */
    public void batchCacheKeywords(Map<String, List<KeywordResponseDTO>> contentKeywordsMap) {
        if (contentKeywordsMap == null || contentKeywordsMap.isEmpty()) {
            log.debug("Skipping batch cache - empty map");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        int validEntries = 0;
        
        try {
            // Pre-filter valid entries to avoid pipeline overhead for invalid data
            Map<String, List<KeywordResponseDTO>> validEntriesMap = new HashMap<>();
            for (Map.Entry<String, List<KeywordResponseDTO>> entry : contentKeywordsMap.entrySet()) {
                String content = entry.getKey();
                List<KeywordResponseDTO> keywords = entry.getValue();
                
                if (content != null && !content.trim().isEmpty() && keywords != null && !keywords.isEmpty()) {
                    validEntriesMap.put(content, keywords);
                    validEntries++;
                }
            }
            
            if (validEntriesMap.isEmpty()) {
                log.debug("No valid entries found for batch caching");
                return;
            }
            
            // Use proper Redis pipeline for batch operations
            log.debug("Starting Redis pipeline for {} valid entries", validEntries);
            
            List<Object> pipelineResults = redisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                @SuppressWarnings("unchecked")
                public Object execute(RedisOperations operations) throws DataAccessException {
                    for (Map.Entry<String, List<KeywordResponseDTO>> entry : validEntriesMap.entrySet()) {
                        String content = entry.getKey();
                        List<KeywordResponseDTO> keywords = entry.getValue();
                        
                        try {
                            String contentHash = generateContentHash(content);
                            String cacheKey = CACHE_PREFIX + contentHash;
                            
                            // Pipeline operation - queued for batch execution
                            operations.opsForValue().set(cacheKey, keywords, DEFAULT_TTL);
                            
                            log.trace("Queued pipeline operation for key: {}", cacheKey);
                            
                        } catch (Exception e) {
                            log.error("Error preparing pipeline operation for content '{}': {}", 
                                    content.substring(0, Math.min(50, content.length())), e.getMessage());
                            // Continue with other entries
                        }
                    }
                    return null; // Return null for pipeline operations
                }
            });
            
            long pipelineTime = System.currentTimeMillis() - startTime;
            cacheStores += validEntries;
            
            log.info("Redis pipeline completed: {} entries cached in {}ms (avg: {:.2f}ms/entry)", 
                    validEntries, pipelineTime, validEntries > 0 ? (double) pipelineTime / validEntries : 0);
            
            // Debug pipeline results if needed
            if (pipelineResults != null && log.isDebugEnabled()) {
                log.debug("Pipeline returned {} results", pipelineResults.size());
            }
            
        } catch (DataAccessException e) {
            long pipelineTime = System.currentTimeMillis() - startTime;
            log.error("Redis DataAccessException during pipeline after {}ms: {}", pipelineTime, e.getMessage(), e);
            
            // Fallback to individual operations if pipeline fails
            log.warn("Falling back to individual cache operations due to Redis access error");
            fallbackToIndividualOperations(contentKeywordsMap);
            
        } catch (Exception e) {
            long pipelineTime = System.currentTimeMillis() - startTime;
            log.error("Unexpected error during Redis pipeline after {}ms: {}", pipelineTime, e.getMessage(), e);
            
            // Fallback to individual operations for any other errors
            log.warn("Falling back to individual cache operations due to unexpected error");
            fallbackToIndividualOperations(contentKeywordsMap);
        }
    }
    
    /**
     * Fallback method for individual cache operations when pipeline fails
     */
    private void fallbackToIndividualOperations(Map<String, List<KeywordResponseDTO>> contentKeywordsMap) {
        long fallbackStartTime = System.currentTimeMillis();
        int fallbackSuccess = 0;
        int fallbackFailures = 0;
        
        for (Map.Entry<String, List<KeywordResponseDTO>> entry : contentKeywordsMap.entrySet()) {
            String content = entry.getKey();
            List<KeywordResponseDTO> keywords = entry.getValue();
            
            try {
                if (content != null && !content.trim().isEmpty() && keywords != null && !keywords.isEmpty()) {
                    cacheKeywords(content, keywords);
                    fallbackSuccess++;
                }
            } catch (Exception e) {
                fallbackFailures++;
                log.error("Error in fallback caching for content '{}': {}", 
                        content.substring(0, Math.min(50, content.length())), e.getMessage());
            }
        }
        
        long fallbackTime = System.currentTimeMillis() - fallbackStartTime;
        log.info("Fallback caching completed: {} successful, {} failed in {}ms", 
                fallbackSuccess, fallbackFailures, fallbackTime);
    }
    
    public static class CacheStatistics {
        private final long hits;
        private final long misses;
        private final long stores;
        private final int totalKeys;
        private final double hitRate;
        private final LocalDateTime timestamp;
        
        public CacheStatistics(long hits, long misses, long stores, int totalKeys, 
                              double hitRate, LocalDateTime timestamp) {
            this.hits = hits;
            this.misses = misses;
            this.stores = stores;
            this.totalKeys = totalKeys;
            this.hitRate = hitRate;
            this.timestamp = timestamp;
        }
        
        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public long getStores() { return stores; }
        public int getTotalKeys() { return totalKeys; }
        public double getHitRate() { return hitRate; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        public long getTotalRequests() { return hits + misses; }
        
        @Override
        public String toString() {
            return String.format("CacheStats{hits=%d, misses=%d, stores=%d, keys=%d, hitRate=%.2f%%, time=%s}",
                    hits, misses, stores, totalKeys, hitRate, timestamp);
        }
    }
}
