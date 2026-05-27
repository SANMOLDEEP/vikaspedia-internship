package com.seo.keywordgenerator.repository;

import com.seo.keywordgenerator.model.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KeywordRepository extends JpaRepository<Keyword, Long> {
    
    List<Keyword> findByContentIdOrderByScoreDesc(String contentId);
    
    Optional<Keyword> findByKeyword(String keyword);
    
    @Query("SELECT k FROM Keyword k WHERE k.keyword LIKE %:term% ORDER BY k.score DESC")
    List<Keyword> findByKeywordContainingIgnoreCaseOrderByScoreDesc(@Param("term") String term);
    
    @Query("SELECT DISTINCT k.keyword FROM Keyword k WHERE k.searchVolume > :threshold ORDER BY k.searchVolume DESC")
    List<String> findTopKeywordsBySearchVolume(@Param("threshold") int threshold);
    
    @Query("SELECT k FROM Keyword k WHERE k.type = :type ORDER BY k.score DESC")
    List<Keyword> findByTypeOrderByScoreDesc(@Param("type") String type);
    
    boolean existsByKeyword(String keyword);

    long countByKeywordIgnoreCase(String keyword);
}
