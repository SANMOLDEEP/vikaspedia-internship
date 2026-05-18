package com.seo.keywordgenerator.service;

import com.seo.keywordgenerator.dto.KeywordResponseDTO;
import com.seo.keywordgenerator.model.SearchAnalytics;
import com.seo.keywordgenerator.repository.SearchAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SearchAnalyticsService {
    
    private final SearchAnalyticsRepository searchAnalyticsRepository;
    
    /**
     * Track keyword search and update analytics
     */
    @CacheEvict(value = {"popular-keywords", "recent-searches"}, allEntries = true)
    public void trackKeywordSearch(String keyword, List<KeywordResponseDTO> generatedKeywords) {
        try {
            Optional<SearchAnalytics> existing = searchAnalyticsRepository.findByKeywordIgnoreCase(keyword.trim());
            
            if (existing.isPresent()) {
                SearchAnalytics analytics = existing.get();
                analytics.incrementSearchCount();
                
                // Update content generation stats
                if (!generatedKeywords.isEmpty()) {
                    double avgScore = generatedKeywords.stream()
                            .mapToDouble(KeywordResponseDTO::getScore)
                            .average()
                            .orElse(0.0);
                    
                    String tier = getPopularityTier(avgScore);
                    analytics.updateContentStats((int) avgScore, tier);
                }
                
                searchAnalyticsRepository.save(analytics);
                log.debug("Updated search analytics for keyword: {}", keyword);
                
            } else {
                // Create new analytics entry
                SearchAnalytics newAnalytics = SearchAnalytics.builder()
                        .keyword(keyword.trim())
                        .searchCount(1L)
                        .lastSearched(LocalDateTime.now())
                        .totalContentGenerated(generatedKeywords.size())
                        .build();
                
                if (!generatedKeywords.isEmpty()) {
                    double avgScore = generatedKeywords.stream()
                            .mapToDouble(KeywordResponseDTO::getScore)
                            .average()
                            .orElse(0.0);
                    
                    newAnalytics.setAverageScore(avgScore);
                    newAnalytics.setPopularityTier(getPopularityTier(avgScore));
                }
                
                searchAnalyticsRepository.save(newAnalytics);
                log.debug("Created new search analytics for keyword: {}", keyword);
            }
            
        } catch (Exception e) {
            log.error("Error tracking keyword search for '{}': {}", keyword, e.getMessage());
        }
    }
    
    /**
     * Get most searched keywords for auto-suggest
     */
    @Cacheable(value = "popular-keywords")
    @Transactional(readOnly = true)
    public List<String> getPopularKeywords(int limit) {
        try {
            List<SearchAnalytics> popular = searchAnalyticsRepository.findMostSearchedKeywords(2L);
            
            return popular.stream()
                    .limit(limit)
                    .map(SearchAnalytics::getKeyword)
                    .toList();
        } catch (Exception e) {
            log.error("Error getting popular keywords: {}", e.getMessage());
            return new ArrayList<>(); // Return empty list on error
        }
    }
    
    /**
     * Get recently searched keywords
     */
    @Cacheable(value = "recent-searches")
    @Transactional(readOnly = true)
    public List<String> getRecentKeywords(int limit) {
        try {
            List<SearchAnalytics> recent = searchAnalyticsRepository.findRecentlySearchedKeywords(
                    LocalDateTime.now().minusDays(7));
            
            return recent.stream()
                    .limit(limit)
                    .map(SearchAnalytics::getKeyword)
                    .distinct()
                    .toList();
        } catch (Exception e) {
            log.error("Error getting recent keywords: {}", e.getMessage());
            return new ArrayList<>(); // Return empty list on error
        }
    }
    
    /**
     * Get keywords by popularity tier
     */
    @Transactional(readOnly = true)
    public List<SearchAnalytics> getKeywordsByTier(String tier) {
        return searchAnalyticsRepository.findByPopularityTier(tier);
    }
    
    /**
     * Get search statistics
     */
    @Transactional(readOnly = true)
    public SearchStats getSearchStats() {
        Long todaySearches = searchAnalyticsRepository.countSearchesSince(
                LocalDateTime.now().minusDays(1));
        
        List<SearchAnalytics> topKeywords = searchAnalyticsRepository.findTopKeywordsBySearchCount();
        
        return SearchStats.builder()
                .totalSearches(todaySearches)
                .topKeywords(topKeywords.stream()
                        .limit(10)
                        .map(SearchAnalytics::getKeyword)
                        .toList())
                .build();
    }
    
    private String getPopularityTier(double score) {
        if (score >= 80) return "HIGH";
        if (score >= 60) return "MEDIUM";
        return "LOW";
    }
    
    @lombok.Data
    @lombok.Builder
    public static class SearchStats {
        private Long totalSearches;
        private List<String> topKeywords;
    }
}
