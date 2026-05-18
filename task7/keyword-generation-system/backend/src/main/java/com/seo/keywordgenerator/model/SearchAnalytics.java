package com.seo.keywordgenerator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_analytics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchAnalytics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "keyword", nullable = false, length = 500)
    private String keyword;
    
    @Column(name = "search_count", nullable = false)
    @Builder.Default
    private Long searchCount = 1L;
    
    @Column(name = "last_searched", nullable = false)
    private LocalDateTime lastSearched;
    
    @Column(name = "total_content_generated", nullable = false)
    @Builder.Default
    private Integer totalContentGenerated = 0;
    
    @Column(name = "average_score", nullable = false)
    @Builder.Default
    private Double averageScore = 0.0;
    
    @Column(name = "popularity_tier", length = 20)
    private String popularityTier; // HIGH, MEDIUM, LOW
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public void incrementSearchCount() {
        this.searchCount++;
        this.lastSearched = LocalDateTime.now();
    }
    
    public void updateContentStats(Integer newScore, String tier) {
        this.totalContentGenerated++;
        // Calculate rolling average
        this.averageScore = (this.averageScore * (this.totalContentGenerated - 1) + newScore) / this.totalContentGenerated;
        this.popularityTier = tier;
    }
}
