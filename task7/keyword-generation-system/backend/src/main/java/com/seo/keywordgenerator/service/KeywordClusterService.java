package com.seo.keywordgenerator.service;

import com.seo.keywordgenerator.dto.KeywordResponseDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class KeywordClusterService {

    private static final Set<String> MODIFIERS = Set.of(
            "how", "to", "learn", "use", "best", "tutorial", "guide", "beginner", "beginners",
            "explained", "setup", "optimization", "practices", "for", "with", "vs", "top"
    );

    public List<KeywordResponseDTO> assignClusters(List<KeywordResponseDTO> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return List.of();
        }

        for (KeywordResponseDTO keyword : keywords) {
            keyword.setCluster(clusterName(keyword));
        }
        return keywords;
    }

    public Map<String, List<KeywordResponseDTO>> clusterKeywords(List<KeywordResponseDTO> keywords) {
        Map<String, List<KeywordResponseDTO>> clusters = new LinkedHashMap<>();
        if (keywords == null || keywords.isEmpty()) {
            return clusters;
        }

        assignClusters(keywords);
        keywords.stream()
                .sorted(Comparator.comparingDouble(KeywordResponseDTO::getScore).reversed())
                .forEach(keyword -> clusters.computeIfAbsent(keyword.getCluster(), ignored -> new ArrayList<>()).add(keyword));

        return clusters;
    }

    private String clusterName(KeywordResponseDTO keyword) {
        String topic = extractTopic(keyword.getKeyword());
        String intent = keyword.getSearchIntent() == null ? "" : keyword.getSearchIntent();

        if ("BEGINNER_FOCUSED".equals(intent) || containsAny(keyword.getKeyword(), "learn", "tutorial", "guide", "beginner")) {
            return title(topic) + " Learning";
        }
        if (containsAny(keyword.getKeyword(), "interview", "questions")) {
            return title(topic) + " Interview";
        }
        if ("COMPARISON".equals(intent) || containsAny(keyword.getKeyword(), " vs ", "best ")) {
            return title(topic) + " Comparison";
        }
        if ("OPTIMIZATION_FOCUSED".equals(intent) || containsAny(keyword.getKeyword(), "optimization", "performance", "best practices", "caching")) {
            return title(topic) + " Optimization";
        }
        if ("TUTORIAL".equals(intent) || containsAny(keyword.getKeyword(), "setup", "how to use")) {
            return title(topic) + " Tutorials";
        }

        return title(topic) + " Research";
    }

    private String extractTopic(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "General";
        }

        List<String> words = new ArrayList<>();
        for (String word : keyword.toLowerCase(Locale.ROOT).split("\\s+")) {
            String cleaned = word.replaceAll("[^a-z0-9+#]", "");
            if (!cleaned.isBlank() && !MODIFIERS.contains(cleaned)) {
                words.add(cleaned);
            }
        }

        if (words.isEmpty()) {
            return "General";
        }

        int limit = Math.min(words.size(), 3);
        return String.join(" ", words.subList(0, limit));
    }

    private boolean containsAny(String keyword, String... parts) {
        String lower = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        for (String part : parts) {
            if (lower.contains(part)) return true;
        }
        return false;
    }

    private String title(String text) {
        if (text == null || text.isBlank()) {
            return "General";
        }

        StringBuilder result = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (word.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            if (word.length() <= 2) {
                result.append(word.toUpperCase(Locale.ROOT));
            } else {
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return result.toString();
    }
}
