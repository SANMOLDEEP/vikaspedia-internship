package com.seo.keywordgenerator.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "keywords")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Keyword {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String keyword;
    
    @Column(nullable = false)
    private double score;
    
    @Column(name = "search_volume")
    private Integer searchVolume;

    @Column(name = "estimated", nullable = false)
    private boolean estimated;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "validation_status", length = 100)
    private String validationStatus;

    @Column(name = "trend_direction")
    private Double trendDirection;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;
    
    @Column(nullable = false, length = 50)
    private String type;
    
    @Column(name = "content_id", nullable = false, length = 255)
    private String contentId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public Keyword(String keyword, double score, String type, String contentId) {
        this.keyword = keyword;
        this.score = score;
        this.type = type;
        this.contentId = contentId;
    }
    
    public Keyword(String keyword, double score, String type, String contentId, Integer searchVolume) {
        this.keyword = keyword;
        this.score = score;
        this.type = type;
        this.contentId = contentId;
        this.searchVolume = searchVolume;
    }

    public Keyword(String keyword, double score, String type, String contentId, Integer searchVolume,
                   boolean estimated, String source, String validationStatus, Double trendDirection,
                   LocalDateTime validatedAt) {
        this(keyword, score, type, contentId, searchVolume);
        this.estimated = estimated;
        this.source = source;
        this.validationStatus = validationStatus;
        this.trendDirection = trendDirection;
        this.validatedAt = validatedAt;
    }
}
