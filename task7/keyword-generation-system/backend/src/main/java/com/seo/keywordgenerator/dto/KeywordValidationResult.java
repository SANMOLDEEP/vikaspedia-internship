package com.seo.keywordgenerator.dto;

import com.seo.keywordgenerator.enums.ValidationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeywordValidationResult {
    
    private String keyword;
    private double popularityScore;
    private long searchVolume;
    private double demandScore;
    private double trendDirection;
    private boolean isValid;
    private String validationStatus; // Keep as String for JSON compatibility
    private Map<String, Long> regionalData;
    private Map<String, Double> relatedKeywords;
    private LocalDateTime validatedAt;
    private boolean estimated;
    private String source;
    private String confidence; // New field: LOW, MEDIUM, HIGH
    private double confidenceScore; // New field: 0.0-1.0
    
    public KeywordValidationResult(String keyword) {
        this.keyword = keyword;
        this.validatedAt = LocalDateTime.now();
    }
    
    public KeywordValidationResult(String keyword, double popularityScore, long searchVolume, 
                                 double demandScore, boolean isValid, String validationStatus) {
        this.keyword = keyword;
        this.popularityScore = popularityScore;
        this.searchVolume = searchVolume;
        this.demandScore = demandScore;
        this.isValid = isValid;
        this.validationStatus = validationStatus;
        this.validatedAt = LocalDateTime.now();
    }

    public KeywordValidationResult(String keyword, double popularityScore, long searchVolume,
                                 double demandScore, boolean isValid, String validationStatus,
                                 boolean estimated, String source) {
        this(keyword, popularityScore, searchVolume, demandScore, isValid, validationStatus);
        this.estimated = estimated;
        this.source = source;
        // Set confidence based on data source
        if (estimated) {
            this.confidence = "LOW";
            this.confidenceScore = 0.3;
        } else {
            this.confidence = "HIGH";
            this.confidenceScore = 0.9;
        }
    }
    
    public boolean isHighDemand() {
        return demandScore >= 70.0;
    }
    
    public boolean isTrendingUp() {
        return trendDirection > 0.1;
    }
    
    public boolean isTrendingDown() {
        return trendDirection < -0.1;
    }
    
    public String getDemandCategory() {
        if (demandScore >= 80.0) return "VERY_HIGH";
        if (demandScore >= 60.0) return "HIGH";
        if (demandScore >= 40.0) return "MEDIUM";
        if (demandScore >= 20.0) return "LOW";
        return "VERY_LOW";
    }
    
    public String getPopularityCategory() {
        if (popularityScore >= 80.0) return "VERY_POPULAR";
        if (popularityScore >= 60.0) return "POPULAR";
        if (popularityScore >= 40.0) return "MODERATE";
        if (popularityScore >= 20.0) return "LOW_POPULARITY";
        return "VERY_LOW_POPULARITY";
    }
}
