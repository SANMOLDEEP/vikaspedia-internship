package com.seo.keywordgenerator.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KeywordResponseDTO {
    
    private String keyword;
    
    private double score;
    
    private int searchVolume;
    
    private String type;

    private boolean estimated;

    private String source;

    private String validationStatus;

    private double trendDirection;

    private double confidenceScore;

    private String searchIntent;

    private String popularityTier;

    private String cluster;

    private long processingTimeMs;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validatedAt;
    
    // Default constructor for JSON deserialization
    public KeywordResponseDTO() {
    }
    
    public KeywordResponseDTO(String keyword, double score, String type) {
        this.keyword = keyword;
        this.score = score;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }
    
    public KeywordResponseDTO(String keyword, double score, String type, int searchVolume) {
        this.keyword = keyword;
        this.score = score;
        this.type = type;
        this.searchVolume = searchVolume;
        this.createdAt = LocalDateTime.now();
    }
}
