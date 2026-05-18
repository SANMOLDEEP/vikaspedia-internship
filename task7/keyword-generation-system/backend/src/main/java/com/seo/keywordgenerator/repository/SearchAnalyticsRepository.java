package com.seo.keywordgenerator.repository;

import com.seo.keywordgenerator.model.SearchAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SearchAnalyticsRepository extends JpaRepository<SearchAnalytics, Long> {
    
    Optional<SearchAnalytics> findByKeywordIgnoreCase(String keyword);
    
    @Query("SELECT sa FROM SearchAnalytics sa WHERE sa.searchCount >= :minCount ORDER BY sa.searchCount DESC")
    List<SearchAnalytics> findMostSearchedKeywords(@Param("minCount") Long minCount);
    
    @Query("SELECT sa FROM SearchAnalytics sa WHERE sa.lastSearched >= :since ORDER BY sa.lastSearched DESC")
    List<SearchAnalytics> findRecentlySearchedKeywords(@Param("since") LocalDateTime since);
    
    @Query("SELECT sa FROM SearchAnalytics sa WHERE sa.popularityTier = :tier ORDER BY sa.averageScore DESC")
    List<SearchAnalytics> findByPopularityTier(@Param("tier") String tier);
    
    @Query("SELECT sa.keyword FROM SearchAnalytics sa WHERE sa.searchCount >= :minCount")
    List<String> findPopularKeywordStrings(@Param("minCount") Long minCount);
    
    @Query("SELECT COUNT(sa) FROM SearchAnalytics sa WHERE sa.lastSearched >= :since")
    Long countSearchesSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT sa FROM SearchAnalytics sa ORDER BY sa.searchCount DESC, sa.averageScore DESC")
    List<SearchAnalytics> findTopKeywordsBySearchCount();
}
