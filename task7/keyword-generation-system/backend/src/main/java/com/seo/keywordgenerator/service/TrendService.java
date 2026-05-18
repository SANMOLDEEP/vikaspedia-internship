package com.seo.keywordgenerator.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public interface TrendService {

    Optional<TrendData> getTrendData(String keyword);

    Optional<TrendData> getTrendData(String keyword, LocalDateTime startDate, LocalDateTime endDate);

    Map<String, TrendData> getBatchTrendData(java.util.List<String> keywords);

    boolean isTrending(String keyword);

    double getPopularityScore(String keyword);
    
    /**
     * Get trend score for keyword with fallback logic
     * @param keyword the keyword to analyze
     * @return trend score (0-100) or estimated score if no cached data
     */
    double getTrendScore(String keyword);

    void cacheTrendData(String keyword, TrendData trendData);

    void invalidateCache(String keyword);

    record TrendData(
            String keyword,
            double popularityScore,
            long searchVolume,
            double trendDirection,
            LocalDateTime lastUpdated,
            Map<String, Long> regionalData,
            Map<String, Double> relatedKeywords,
            String source,
            boolean estimated
    ) {
        public TrendData {
            if (keyword == null || keyword.trim().isEmpty()) {
                throw new IllegalArgumentException("Keyword cannot be null or empty");
            }
            if (popularityScore < 0 || popularityScore > 100) {
                throw new IllegalArgumentException("Popularity score must be between 0 and 100");
            }
        }
    }
}
