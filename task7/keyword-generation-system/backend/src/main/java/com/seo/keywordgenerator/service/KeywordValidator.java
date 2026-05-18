package com.seo.keywordgenerator.service;

import com.seo.keywordgenerator.dto.KeywordValidationResult;
import com.seo.keywordgenerator.enums.ValidationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeywordValidator {

    private final TrendService trendService;
    
    private static final double HIGH_DEMAND_THRESHOLD = 70.0;
    private static final double MEDIUM_DEMAND_THRESHOLD = 40.0;
    private static final double TRENDING_UP_THRESHOLD = 0.1;
    private static final double TRENDING_DOWN_THRESHOLD = -0.1;
    
    // Score normalization constants
    private static final double EXCELLENT_MIN_SCORE = 80.0;
    private static final double GOOD_MIN_SCORE = 60.0;
    private static final double AVERAGE_MIN_SCORE = 40.0;
    private static final double MINIMUM_ACCEPTABLE_SCORE = 40.0; // Filter out weak keywords
    
    // Keyword length constraints (SEO best practices)
    private static final int MIN_WORD_COUNT = 2;  // Enforce long-tail keywords
    private static final int MAX_WORD_COUNT = 5;  // Keep readable
    
    // Dynamic normalization smoothing constant
    private static final double NORMALIZATION_SMOOTHING_CONSTANT = 50.0; // Prevents extreme scaling
    
    private static final Map<String, Double> KEYWORD_MODIFIER_WEIGHTS = Map.ofEntries(
            Map.entry("best", 1.2),
            Map.entry("top", 1.15),
            Map.entry("guide", 1.1),
            Map.entry("tutorial", 1.1),
            Map.entry("ultimate", 1.25),
            Map.entry("complete", 1.2),
            Map.entry("advanced", 1.15),
            Map.entry("professional", 1.2),
            Map.entry("expert", 1.25),
            Map.entry("step by step", 1.1),
            Map.entry("for beginners", 1.05),
            Map.entry("easy", 1.0),
            Map.entry("quick", 0.95),
            Map.entry("free", 0.8),
            Map.entry("cheap", 0.7),
            Map.entry("online", 1.05),
            Map.entry("course", 1.1),
            Map.entry("training", 1.15),
            Map.entry("certification", 1.2)
    );

    public KeywordValidationResult validateKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return createInvalidResult(keyword, "Keyword is null or empty");
        }
        
        String normalizedKeyword = keyword.toLowerCase().trim();
        
        try {
            Optional<TrendService.TrendData> trendData = trendService.getTrendData(normalizedKeyword);
            
            if (trendData.isEmpty()) {
                return createNoDataResult(normalizedKeyword);
            }
            
            return createValidationResult(normalizedKeyword, trendData.get());
            
        } catch (Exception e) {
            log.error("Error validating keyword '{}': {}", keyword, e.getMessage());
            return createErrorResult(normalizedKeyword, e.getMessage());
        }
    }

    public List<KeywordValidationResult> validateKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return Collections.emptyList();
        }
        
        return keywords.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .map(this::validateKeyword)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "keywordValidation", key = "#keyword")
    public KeywordValidationResult validateKeywordWithCache(String keyword) {
        return validateKeyword(keyword);
    }

    public List<KeywordValidationResult> validateKeywordsWithBatchProcessing(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> normalizedKeywords = keywords.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());
        
        Map<String, TrendService.TrendData> batchTrendData = trendService.getBatchTrendData(normalizedKeywords);
        
        return normalizedKeywords.stream()
                .map(keyword -> {
                    TrendService.TrendData trendData = batchTrendData.get(keyword);
                    if (trendData != null) {
                        return createValidationResult(keyword, trendData);
                    } else {
                        return createNoDataResult(keyword);
                    }
                })
                .collect(Collectors.toList());
    }

    public List<KeywordValidationResult> getValidKeywords(List<String> keywords) {
        return validateKeywords(keywords).stream()
                .filter(KeywordValidationResult::isValid)
                .collect(Collectors.toList());
    }

    public List<KeywordValidationResult> getHighDemandKeywords(List<String> keywords) {
        return validateKeywords(keywords).stream()
                .filter(KeywordValidationResult::isValid)
                .filter(KeywordValidationResult::isHighDemand)
                .collect(Collectors.toList());
    }

    public List<KeywordValidationResult> getTrendingKeywords(List<String> keywords) {
        return validateKeywords(keywords).stream()
                .filter(KeywordValidationResult::isValid)
                .filter(KeywordValidationResult::isTrendingUp)
                .collect(Collectors.toList());
    }

    public Map<String, List<KeywordValidationResult>> categorizeKeywordsByDemand(List<String> keywords) {
        List<KeywordValidationResult> validationResults = validateKeywords(keywords);
        
        return validationResults.stream()
                .collect(Collectors.groupingBy(
                        KeywordValidationResult::getDemandCategory,
                        Collectors.toList()
                ));
    }

    public KeywordValidationResult validateAndScoreKeyword(String keyword) {
        KeywordValidationResult result = validateKeyword(keyword);
        
        // No enhanced scoring needed - use HybridTrendService scores directly
        return result;
    }

    private KeywordValidationResult createValidationResult(String keyword, TrendService.TrendData trendData) {
        // Use HybridTrendService popularity score directly
        double popularityScore = trendData.popularityScore();
        boolean isValid = isValidKeyword(trendData, popularityScore);
        String validationStatus = determineValidationStatus(trendData, popularityScore, isValid);
        
        KeywordValidationResult result = new KeywordValidationResult(
                keyword,
                trendData.popularityScore(),
                trendData.searchVolume(),
                popularityScore,
                isValid,
                validationStatus,
                trendData.estimated(),
                trendData.source()
        );
        
        result.setTrendDirection(trendData.trendDirection());
        result.setRegionalData(trendData.regionalData());
        result.setRelatedKeywords(trendData.relatedKeywords());
        
        return result;
    }

    private boolean isValidKeyword(TrendService.TrendData trendData, double popularityScore) {
        boolean autocompleteVerified = "GOOGLE_AUTOCOMPLETE".equals(trendData.source());
        boolean hasVolumeWhenRequired = autocompleteVerified || trendData.searchVolume() >= 100;

        return trendData.popularityScore() >= 10.0 &&
               hasVolumeWhenRequired &&
               popularityScore >= MEDIUM_DEMAND_THRESHOLD;
    }

    private String determineValidationStatus(TrendService.TrendData trendData, double popularityScore, boolean isValid) {
        if (!isValid) {
            if (trendData.popularityScore() < 10.0) return ValidationStatus.LOW_POPULARITY.toString();
            if (trendData.searchVolume() < 100 && !"GOOGLE_AUTOCOMPLETE".equals(trendData.source())) return ValidationStatus.LOW_VOLUME.toString();
            if (popularityScore < MEDIUM_DEMAND_THRESHOLD) return ValidationStatus.LOW_DEMAND.toString();
            return ValidationStatus.REJECTED.toString();
        }
        
        if (trendData.estimated()) {
            return ValidationStatus.ESTIMATED.toString();
        }
        
        return ValidationStatus.VALID.toString();
    }

    private KeywordValidationResult createInvalidResult(String keyword, String reason) {
        ValidationStatus status = ValidationStatus.INVALID_INPUT;
        if (reason.contains("LOW_POPULARITY")) status = ValidationStatus.LOW_POPULARITY;
        else if (reason.contains("LOW_VOLUME")) status = ValidationStatus.LOW_VOLUME;
        else if (reason.contains("LOW_DEMAND")) status = ValidationStatus.LOW_DEMAND;
        
        return new KeywordValidationResult(keyword, 0.0, 0L, 0.0, false, status.toString(), true, "INVALID_INPUT");
    }

    private KeywordValidationResult createNoDataResult(String keyword) {
        // Use fallback logic when no trend data is available
        double estimatedTrendScore = trendService.getTrendScore(keyword);
        long estimatedSearchVolume = estimateSearchVolumeFromKeyword(keyword);
        double estimatedDemandScore = calculateEstimatedDemandScore(keyword, estimatedTrendScore, estimatedSearchVolume);
        
        boolean isValid = estimatedDemandScore >= MEDIUM_DEMAND_THRESHOLD;
        ValidationStatus validationStatus = isValid ? ValidationStatus.ESTIMATED : ValidationStatus.REJECTED;
        
        KeywordValidationResult result = new KeywordValidationResult(
                keyword,
                estimatedTrendScore,
                estimatedSearchVolume,
                estimatedDemandScore,
                isValid,
                validationStatus.toString(),
                true,
                "ESTIMATED_FALLBACK"
        );
        
        // Set estimated trend direction
        double estimatedTrendDirection = estimateTrendDirection(keyword);
        result.setTrendDirection(estimatedTrendDirection);
        
        log.debug("Created estimated validation result for keyword '{}': score={}, volume={}, valid={}", 
                keyword, estimatedTrendScore, estimatedSearchVolume, isValid);
        
        return result;
    }

    /**
     * Estimate search volume from keyword characteristics when no data is available
     */
    private long estimateSearchVolumeFromKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return 0L;
        }
        
        String[] words = keyword.toLowerCase().trim().split("\\s+");
        long baseVolume;
        
        // REALISTIC search volume ranges
        if (words.length == 1) {
            // Short keywords: 200k - 500k
            baseVolume = deterministicRange(keyword, 200000L, 500000L);
        } else if (words.length == 2) {
            // Medium keywords: 50k - 200k
            baseVolume = deterministicRange(keyword, 50000L, 200000L);
        } else {
            // Long-tail keywords: 1k - 50k
            baseVolume = deterministicRange(keyword, 1000L, 50000L);
        }
        
        // Adjust for technology keywords (slight boost, not 2x)
        if (containsTechTerms(keyword)) {
            baseVolume = (long)(baseVolume * 1.3); // 30% boost for tech terms
        }
        
        // Add realistic variation (±20%)
        baseVolume += 0L;
        
        // Ensure minimum volume
        baseVolume = Math.max(baseVolume, 1000L);
        
        return baseVolume;
    }

    private long deterministicRange(String keyword, long minInclusive, long maxInclusive) {
        long range = Math.max(1L, maxInclusive - minInclusive + 1L);
        long hash = Integer.toUnsignedLong(Objects.hashCode(keyword == null ? "" : keyword.toLowerCase(Locale.ROOT)));
        return minInclusive + (hash % range);
    }

    /**
     * Calculate estimated demand score when no trend data is available
     */
    private double calculateEstimatedDemandScore(String keyword, double trendScore, long searchVolume) {
        double baseScore = trendScore;
        
        // Volume contribution (normalized)
        double volumeScore = Math.min(searchVolume / 100000.0 * 20.0, 20.0);
        
        double totalScore = baseScore + volumeScore;
        
        // Normalize to 0-100 scale and round for clean output
        return normalizeAndRoundScore(totalScore);
    }

    /**
     * Estimate trend direction when no data is available
     */
    private double estimateTrendDirection(String keyword) {
        double baseTrend = 0.0;
        
        // Technology keywords tend to trend up
        if (containsTechTerms(keyword)) {
            baseTrend += 0.1;
        }
        
        // Learning keywords have positive trend
        if (containsLearningTerms(keyword)) {
            baseTrend += 0.05;
        }
        
        // Add some realistic variation
        long hash = Integer.toUnsignedLong(Objects.hashCode(keyword == null ? "" : keyword.toLowerCase(Locale.ROOT)));
        baseTrend += ((hash % 200) / 1000.0) - 0.1;
        
        return Math.max(-0.3, Math.min(0.3, baseTrend));
    }

    /**
     * Check if keyword contains technology terms
     */
    private boolean containsTechTerms(String keyword) {
        String[] techTerms = {"react", "javascript", "python", "java", "node", "api", "database", 
                              "cloud", "docker", "kubernetes", "aws", "vue", "angular", "typescript",
                              "frontend", "backend", "devops", "security", "testing", "css", "html"};
        return Arrays.stream(techTerms).anyMatch(keyword.toLowerCase()::contains);
    }

    /**
     * Check if keyword contains learning terms
     */
    private boolean containsLearningTerms(String keyword) {
        String[] learningTerms = {"tutorial", "learn", "course", "training", "guide", "beginners", 
                                 "master", "advanced", "certification", "bootcamp"};
        return Arrays.stream(learningTerms).anyMatch(keyword.toLowerCase()::contains);
    }

    /**
     * Normalize score to 0-100 scale and round for clean output
     * Uses dynamic normalization: normalized = (totalScore / (totalScore + 50)) * 100
     * Engineering reasoning: Smoothing constant (50) prevents extreme scaling and ensures 
     * diminishing returns for very high scores, keeping values realistic and stable
     * Then round to 1 decimal place for clean display
     */
    private double normalizeAndRoundScore(double totalScore) {
        // Dynamic normalization with smoothing constant
        double normalized = (totalScore / (totalScore + NORMALIZATION_SMOOTHING_CONSTANT)) * 100.0;
        
        // Ensure within bounds
        normalized = Math.max(0.0, Math.min(100.0, normalized));
        
        // Round to 1 decimal place for clean output (e.g., 71.7 instead of 71.72)
        double rounded = Math.round(normalized * 10.0) / 10.0;
        
        return rounded;
    }

    /**
     * Check if score meets minimum threshold (safer than returning null)
     */
    private boolean meetsMinimumThreshold(double score) {
        return score >= MINIMUM_ACCEPTABLE_SCORE;
    }

    /**
     * Check if keyword length is acceptable (SEO best practices)
     */
    private boolean hasValidLength(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        
        int wordCount = keyword.trim().split("\\s+").length;
        return wordCount >= MIN_WORD_COUNT && wordCount <= MAX_WORD_COUNT;
    }

    /**
     * Remove duplicate words from keyword (e.g., "learn learn react" → "learn react")
     * This is crucial for keyword quality and evaluation
     */
    private String removeDuplicateWords(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return keyword;
        }
        
        String[] words = keyword.toLowerCase().trim().split("\\s+");
        Set<String> seenWords = new LinkedHashSet<>(); // Maintains order
        List<String> uniqueWords = new ArrayList<>();
        
        for (String word : words) {
            if (!seenWords.contains(word) && !word.isEmpty()) {
                seenWords.add(word);
                uniqueWords.add(word);
            }
        }
        
        return String.join(" ", uniqueWords);
    }

    /**
     * Enhanced validation with duplicate removal and quality constraints
     */
    public KeywordValidationResult validateKeywordWithCleaning(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return createInvalidResult(keyword, "Keyword is null or empty");
        }
        
        // Step 1: Remove duplicate words
        String cleanedKeyword = removeDuplicateWords(keyword.trim());
        
        // Step 2: Check length constraints (SEO best practices)
        if (!hasValidLength(cleanedKeyword)) {
            return createInvalidResult(keyword, 
                String.format("Keyword length invalid: must be %d-%d words", MIN_WORD_COUNT, MAX_WORD_COUNT));
        }
        
        // Step 3: Get trend data for cleaned keyword
        String normalizedKeyword = cleanedKeyword.toLowerCase().trim();
        
        try {
            Optional<TrendService.TrendData> trendData = trendService.getTrendData(normalizedKeyword);
            
            if (trendData.isEmpty()) {
                return createNoDataResultWithThreshold(normalizedKeyword);
            }
            
            return createValidationResultWithThreshold(normalizedKeyword, trendData.get());
            
        } catch (Exception e) {
            log.error("Error validating keyword '{}': {}", keyword, e.getMessage());
            return createErrorResult(normalizedKeyword, e.getMessage());
        }
    }

    /**
     * Create validation result with threshold filtering (safer - no null returns)
     */
    private KeywordValidationResult createValidationResultWithThreshold(String keyword, TrendService.TrendData trendData) {
        double totalScore = calculateRawScore(keyword, trendData);
        double normalizedScore = normalizeAndRoundScore(totalScore);
        
        // Check threshold and return structured result (safer than null)
        if (!meetsMinimumThreshold(normalizedScore)) {
            return createInvalidResult(keyword, 
                String.format("Score %.1f below minimum threshold %.1f", normalizedScore, MINIMUM_ACCEPTABLE_SCORE));
        }
        
        boolean isValid = true;
        String validationStatus = determineValidationStatus(trendData, normalizedScore, isValid);
        
        KeywordValidationResult result = new KeywordValidationResult(
                keyword,
                trendData.popularityScore(),
                trendData.searchVolume(),
                normalizedScore,
                isValid,
                validationStatus,
                trendData.estimated(),
                trendData.source()
        );
        
        result.setTrendDirection(trendData.trendDirection());
        result.setRegionalData(trendData.regionalData());
        result.setRelatedKeywords(trendData.relatedKeywords());
        
        return result;
    }

    /**
     * Create no data result with threshold filtering (safer - no null returns)
     */
    private KeywordValidationResult createNoDataResultWithThreshold(String keyword) {
        double estimatedTrendScore = trendService.getTrendScore(keyword);
        long estimatedSearchVolume = estimateSearchVolumeFromKeyword(keyword);
        double totalScore = calculateRawEstimatedScore(keyword, estimatedTrendScore, estimatedSearchVolume);
        double normalizedScore = normalizeAndRoundScore(totalScore);
        
        // Check threshold and return structured result (safer than null)
        if (!meetsMinimumThreshold(normalizedScore)) {
            return createInvalidResult(keyword, 
                String.format("Estimated score %.1f below minimum threshold %.1f", normalizedScore, MINIMUM_ACCEPTABLE_SCORE));
        }
        
        boolean isValid = true;
        String validationStatus = "ESTIMATED_VALID";
        
        KeywordValidationResult result = new KeywordValidationResult(
                keyword,
                estimatedTrendScore,
                estimatedSearchVolume,
                normalizedScore,
                isValid,
                validationStatus,
                true,
                "ESTIMATED_FALLBACK"
        );
        
        double estimatedTrendDirection = estimateTrendDirection(keyword);
        result.setTrendDirection(estimatedTrendDirection);
        
        log.debug("Created estimated validation result for keyword '{}': score={}, volume={}, valid={}", 
                keyword, estimatedTrendScore, estimatedSearchVolume, isValid);
        
        return result;
    }

    /**
     * Calculate raw score without normalization (for internal use)
     */
    private double calculateRawScore(String keyword, TrendService.TrendData trendData) {
        double baseScore = trendData.popularityScore();
        double volumeScore = Math.min(trendData.searchVolume() / 1000000.0 * 20.0, 20.0);
        
        return baseScore + volumeScore;
    }

    /**
     * Calculate raw estimated score without normalization (for internal use)
     */
    private double calculateRawEstimatedScore(String keyword, double trendScore, long searchVolume) {
        double baseScore = trendScore;
        double volumeScore = Math.min(searchVolume / 100000.0 * 20.0, 20.0);
        
        return baseScore + volumeScore;
    }

    /**
     * Get score interpretation for viva/explanation
     * @param score normalized score (0-100)
     * @return interpretation string
     */
    public String getScoreInterpretation(double score) {
        if (score >= EXCELLENT_MIN_SCORE) {
            return "Excellent keyword (80-100)";
        } else if (score >= GOOD_MIN_SCORE) {
            return "Good keyword (60-80)";
        } else if (score >= AVERAGE_MIN_SCORE) {
            return "Average keyword (40-60)";
        } else {
            return "Weak keyword (<40)";
        }
    }

    /**
     * Get score category for classification
     * @param score normalized score (0-100)
     * @return category string
     */
    public String getScoreCategory(double score) {
        if (score >= EXCELLENT_MIN_SCORE) {
            return "EXCELLENT";
        } else if (score >= GOOD_MIN_SCORE) {
            return "GOOD";
        } else if (score >= AVERAGE_MIN_SCORE) {
            return "AVERAGE";
        } else {
            return "WEAK";
        }
    }

    private KeywordValidationResult createErrorResult(String keyword, String errorMessage) {
        return new KeywordValidationResult(keyword, 0.0, 0L, 0.0, false, "ERROR: " + errorMessage, true, "VALIDATION_ERROR");
    }

    public ValidationStatistics getValidationStatistics(List<String> keywords) {
        List<KeywordValidationResult> results = validateKeywords(keywords);
        
        long total = results.size();
        long valid = results.stream().mapToLong(r -> r.isValid() ? 1 : 0).sum();
        long highDemand = results.stream().mapToLong(r -> r.isHighDemand() ? 1 : 0).sum();
        long trending = results.stream().mapToLong(r -> r.isTrendingUp() ? 1 : 0).sum();
        
        double avgPopularity = results.stream()
                .mapToDouble(KeywordValidationResult::getPopularityScore)
                .average()
                .orElse(0.0);
        
        double avgDemand = results.stream()
                .mapToDouble(KeywordValidationResult::getDemandScore)
                .average()
                .orElse(0.0);
        
        return new ValidationStatistics(total, valid, highDemand, trending, avgPopularity, avgDemand, LocalDateTime.now());
    }

    public static class ValidationStatistics {
        private final long totalKeywords;
        private final long validKeywords;
        private final long highDemandKeywords;
        private final long trendingKeywords;
        private final double averagePopularity;
        private final double averageDemand;
        private final LocalDateTime timestamp;
        
        public ValidationStatistics(long totalKeywords, long validKeywords, long highDemandKeywords,
                                  long trendingKeywords, double averagePopularity, double averageDemand,
                                  LocalDateTime timestamp) {
            this.totalKeywords = totalKeywords;
            this.validKeywords = validKeywords;
            this.highDemandKeywords = highDemandKeywords;
            this.trendingKeywords = trendingKeywords;
            this.averagePopularity = averagePopularity;
            this.averageDemand = averageDemand;
            this.timestamp = timestamp;
        }
        
        public double getValidationRate() {
            return totalKeywords > 0 ? (double) validKeywords / totalKeywords * 100 : 0.0;
        }
        
        public double getHighDemandRate() {
            return totalKeywords > 0 ? (double) highDemandKeywords / totalKeywords * 100 : 0.0;
        }
        
        public double getTrendingRate() {
            return totalKeywords > 0 ? (double) trendingKeywords / totalKeywords * 100 : 0.0;
        }
        
        // Getters
        public long getTotalKeywords() { return totalKeywords; }
        public long getValidKeywords() { return validKeywords; }
        public long getHighDemandKeywords() { return highDemandKeywords; }
        public long getTrendingKeywords() { return trendingKeywords; }
        public double getAveragePopularity() { return averagePopularity; }
        public double getAverageDemand() { return averageDemand; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("ValidationStats{total=%d, valid=%d, highDemand=%d, trending=%d, " +
                            "avgPop=%.1f, avgDemand=%.1f, validationRate=%.1f%%}",
                    totalKeywords, validKeywords, highDemandKeywords, trendingKeywords,
                    averagePopularity, averageDemand, getValidationRate());
        }
    }
}
