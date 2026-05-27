package com.seo.keywordgenerator.service;

import com.seo.keywordgenerator.dto.KeywordResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeywordClusteringService {

    private final KeywordClusterService keywordClusterService;
    
    /**
     * Cluster keywords by similarity and semantic meaning
     */
    public Map<String, List<KeywordResponseDTO>> clusterKeywords(List<KeywordResponseDTO> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return new HashMap<>();
        }

        return keywordClusterService.clusterKeywords(keywords);
    }
    
    /**
     * Cluster keywords by common words and prefixes
     */
    private Map<String, List<KeywordResponseDTO>> clusterByCommonWords(List<KeywordResponseDTO> keywords) {
        Map<String, List<KeywordResponseDTO>> clusters = new HashMap<>();
        
        for (KeywordResponseDTO keyword : keywords) {
            String keywordText = keyword.getKeyword().toLowerCase();
            
            // Extract main concept words (skip common modifiers)
            String[] words = keywordText.split("\\s+");
            String mainWord = getMainConcept(words);
            
            if (mainWord != null) {
                clusters.computeIfAbsent(mainWord, k -> new ArrayList<>()).add(keyword);
            }
        }
        
        return clusters;
    }
    
    /**
     * Cluster keywords by semantic patterns
     */
    private Map<String, List<KeywordResponseDTO>> clusterByPatterns(List<KeywordResponseDTO> keywords) {
        Map<String, List<KeywordResponseDTO>> clusters = new HashMap<>();
        
        for (KeywordResponseDTO keyword : keywords) {
            String keywordText = keyword.getKeyword().toLowerCase();
            
            // Pattern-based clustering
            if (keywordText.contains("tutorial") || keywordText.contains("guide") || keywordText.contains("learn")) {
                clusters.computeIfAbsent("Learning & Tutorials", k -> new ArrayList<>()).add(keyword);
            } else if (keywordText.contains("best") || keywordText.contains("top") || keywordText.contains("ultimate")) {
                clusters.computeIfAbsent("Best & Top Lists", k -> new ArrayList<>()).add(keyword);
            } else if (keywordText.contains("how to") || keywordText.contains("step by step")) {
                clusters.computeIfAbsent("How-To Guides", k -> new ArrayList<>()).add(keyword);
            } else if (keywordText.matches(".*\\d+.*") || keywordText.contains("under") || keywordText.contains("over")) {
                clusters.computeIfAbsent("Price & Comparison", k -> new ArrayList<>()).add(keyword);
            }
        }
        
        return clusters;
    }
    
    /**
     * Cluster keywords by popularity tiers
     */
    private Map<String, List<KeywordResponseDTO>> clusterByPopularity(List<KeywordResponseDTO> keywords) {
        Map<String, List<KeywordResponseDTO>> clusters = new HashMap<>();
        
        for (KeywordResponseDTO keyword : keywords) {
            String tier = getPopularityTier(keyword.getScore());
            clusters.computeIfAbsent(tier + " Popularity", k -> new ArrayList<>()).add(keyword);
        }
        
        return clusters;
    }
    
    /**
     * Get the main concept word from a keyword
     */
    private String getMainConcept(String[] words) {
        // Filter out common modifiers and stop words
        Set<String> stopWords = Set.of("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "is", "are", "was", "were");
        Set<String> modifiers = Set.of("best", "top", "ultimate", "how", "to", "learn", "tutorial", "guide", "step", "by");
        
        for (String word : words) {
            word = word.replaceAll("[^a-zA-Z]", ""); // Remove special characters
            if (!stopWords.contains(word) && !modifiers.contains(word) && word.length() > 2) {
                return word;
            }
        }
        
        return words.length > 0 ? words[0].replaceAll("[^a-zA-Z]", "") : null;
    }
    
    /**
     * Get popularity tier based on score
     */
    private String getPopularityTier(double score) {
        if (score >= 80) return "High";
        if (score >= 60) return "Medium";
        return "Low";
    }
    
    /**
     * Clean and merge similar clusters
     */
    private Map<String, List<KeywordResponseDTO>> cleanAndMergeClusters(Map<String, List<KeywordResponseDTO>> clusters) {
        Map<String, List<KeywordResponseDTO>> cleaned = new HashMap<>();
        
        // Remove clusters with less than 2 items
        clusters.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= 2)
                .forEach(entry -> cleaned.put(entry.getKey(), entry.getValue()));
        
        // Sort keywords within each cluster by score
        cleaned.values().forEach(list -> 
            list.sort((a, b) -> Double.compare(b.getScore(), a.getScore()))
        );
        
        return cleaned;
    }
    
    /**
     * Get cluster summary statistics
     */
    public ClusterSummary getClusterSummary(Map<String, List<KeywordResponseDTO>> clusters) {
        int totalClusters = clusters.size();
        int totalKeywords = clusters.values().stream().mapToInt(List::size).sum();
        
        Map<String, Integer> clusterSizes = clusters.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size()
                ));
        
        // Find largest cluster
        String largestCluster = clusters.entrySet().stream()
                .max(Comparator.comparingInt(entry -> entry.getValue().size()))
                .map(Map.Entry::getKey)
                .orElse("None");
        
        return ClusterSummary.builder()
                .totalClusters(totalClusters)
                .totalKeywords(totalKeywords)
                .clusterSizes(clusterSizes)
                .largestCluster(largestCluster)
                .averageClusterSize(totalClusters > 0 ? (double) totalKeywords / totalClusters : 0.0)
                .build();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ClusterSummary {
        private int totalClusters;
        private int totalKeywords;
        private Map<String, Integer> clusterSizes;
        private String largestCluster;
        private double averageClusterSize;
    }
}
