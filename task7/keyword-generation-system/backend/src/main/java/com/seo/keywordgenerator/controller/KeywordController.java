package com.seo.keywordgenerator.controller;

import com.seo.keywordgenerator.dto.KeywordRequestDTO;
import com.seo.keywordgenerator.dto.KeywordResponseDTO;
import com.seo.keywordgenerator.service.KeywordClusteringService;
import com.seo.keywordgenerator.service.KeywordService;
import com.seo.keywordgenerator.service.SearchAnalyticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/keywords")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class KeywordController {

    private final KeywordService keywordService;
    private final SearchAnalyticsService searchAnalyticsService;
    private final KeywordClusteringService keywordClusteringService;

    @PostMapping("/generate-keywords")
    public ResponseEntity<List<KeywordResponseDTO>> generateKeywords(@Valid @RequestBody KeywordRequestDTO request) {
        log.info("Received request to generate keywords for content length: {}", request.getContent().length());
        
        try {
            List<KeywordResponseDTO> keywords = keywordService.generateKeywords(request);
            log.info("Successfully generated {} keywords", keywords.size());
            
            // Track search analytics asynchronously
            searchAnalyticsService.trackKeywordSearch(extractMainKeyword(request.getContent()), keywords);
            
            return ResponseEntity.ok(keywords);
        } catch (Exception e) {
            log.error("Error generating keywords: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/clear-cache")
    public ResponseEntity<Map<String, String>> clearCache() {
        log.info("Received request to clear keyword cache");
        
        try {
            // This will require adding a clearCache method to KeywordService
            keywordService.clearCache();
            Map<String, String> response = new HashMap<>();
            response.put("message", "Cache cleared successfully");
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error clearing cache: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/suggest")
    public ResponseEntity<Map<String, Object>> autoSuggest(@RequestParam String query, 
                                                          @RequestParam(defaultValue = "5") int limit) {
        log.info("Received auto-suggest request for query: '{}'", query);
        
        try {
            // Get popular keywords that match the query
            List<String> popularKeywords = searchAnalyticsService.getPopularKeywords(20);
            List<String> recentKeywords = searchAnalyticsService.getRecentKeywords(20);
            
            // Filter keywords that contain query (more flexible matching)
            List<String> popularSuggestions = popularKeywords.stream()
                    .filter(keyword -> keyword.toLowerCase().contains(query.toLowerCase()) || 
                                       query.toLowerCase().contains(keyword.toLowerCase()))
                    .limit(limit)
                    .toList();
            
            // If not enough from popular, add from recent
            List<String> suggestions = popularSuggestions;
            final List<String> currentSuggestions = suggestions;
            if (suggestions.size() < limit) {
                List<String> additional = recentKeywords.stream()
                        .filter(keyword -> keyword.toLowerCase().contains(query.toLowerCase()) || 
                                           query.toLowerCase().contains(keyword.toLowerCase()))
                        .filter(keyword -> !currentSuggestions.contains(keyword))
                        .limit(limit - currentSuggestions.size())
                        .toList();
                
                // Create mutable list and add all suggestions
                List<String> allSuggestions = new ArrayList<>(suggestions);
                allSuggestions.addAll(additional);
                suggestions = allSuggestions;
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
            response.put("suggestions", suggestions);
            response.put("count", suggestions.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in auto-suggest for query '{}': {}", query, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/cluster")
    public ResponseEntity<Map<String, Object>> clusterKeywords(@RequestParam String content) {
        log.info("Received request to cluster keywords for content length: {}", content.length());
        
        try {
            // Generate keywords first
            KeywordRequestDTO request = new KeywordRequestDTO();
            request.setContent(content);
            request.setMaxKeywords(30);
            request.setIncludeLongTail(true);
            
            List<KeywordResponseDTO> keywords = keywordService.generateKeywords(request);
            
            // Cluster the keywords
            Map<String, List<KeywordResponseDTO>> clusters = keywordClusteringService.clusterKeywords(keywords);
            KeywordClusteringService.ClusterSummary summary = keywordClusteringService.getClusterSummary(clusters);
            
            Map<String, Object> response = new HashMap<>();
            response.put("clusters", clusters);
            response.put("summary", summary);
            response.put("totalKeywords", keywords.size());
            response.put("clusteredKeywords", clusters.values().stream().mapToInt(List::size).sum());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error clustering keywords: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/analytics/stats")
    public ResponseEntity<SearchAnalyticsService.SearchStats> getSearchStats() {
        log.info("Received request for search statistics");
        
        try {
            SearchAnalyticsService.SearchStats stats = searchAnalyticsService.getSearchStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching search statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/content/{contentId}")
    public ResponseEntity<List<KeywordResponseDTO>> getKeywordsByContentId(@PathVariable String contentId) {
        log.info("Retrieving keywords for contentId: {}", contentId);
        
        try {
            List<KeywordResponseDTO> keywords = keywordService.getKeywordsByContentId(contentId);
            return ResponseEntity.ok(keywords);
        } catch (Exception e) {
            log.error("Error retrieving keywords for contentId {}: {}", contentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<KeywordResponseDTO>> searchKeywords(@RequestParam String term) {
        log.info("Searching keywords with term: {}", term);
        
        try {
            List<KeywordResponseDTO> keywords = keywordService.searchKeywords(term);
            return ResponseEntity.ok(keywords);
        } catch (Exception e) {
            log.error("Error searching keywords with term {}: {}", term, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "running");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    private String extractMainKeyword(String content) {
        // Simple extraction: take first few words and clean up
        String[] words = content.trim().split("\\s+");
        if (words.length == 0) return "unknown";
        
        // Take first 3-4 words as main keyword
        int wordCount = Math.min(4, words.length);
        String mainKeyword = String.join(" ", java.util.Arrays.copyOf(words, wordCount));
        
        // Clean up common starting words and truncate if too long
        mainKeyword = mainKeyword.replaceAll("^(This|The|A|An)\\s+", "").trim();
        
        // Limit to reasonable length (max 50 characters)
        if (mainKeyword.length() > 50) {
            mainKeyword = mainKeyword.substring(0, 47) + "...";
        }
        
        return mainKeyword;
    }

    @GetMapping("/metrics")
    public ResponseEntity<Object> getMetrics() {
        try {
            return ResponseEntity.ok(keywordService.getPipelineMetrics());
        } catch (Exception e) {
            log.error("Error retrieving metrics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
