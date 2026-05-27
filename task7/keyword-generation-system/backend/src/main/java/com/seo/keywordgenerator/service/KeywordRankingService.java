package com.seo.keywordgenerator.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class KeywordRankingService {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "from", "in", "into",
            "is", "it", "of", "on", "or", "that", "the", "their", "then", "there", "these", "they",
            "this", "to", "was", "will", "with", "how", "what", "why", "when", "where"
    );

    public List<RankedKeyword> rank(List<KeywordOptimizationService.KeywordCandidate> candidates,
                                    KeywordExtractionService.ExtractionResult extraction,
                                    int maxKeywords) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        Map<String, RankedKeyword> rankedByKeyword = new LinkedHashMap<>();
        for (KeywordOptimizationService.KeywordCandidate candidate : candidates) {
            double naturalnessScore = phraseNaturalnessScore(candidate.keyword());
            if (naturalnessScore < 40.0) {
                continue;
            }

            double score = candidate.qualityScore();
            int words = wordCount(candidate.keyword());
            if (words >= 3 && words <= 6) score += 18.0;
            if (words == 1) score -= 35.0;
            if ("LONG_TAIL".equals(candidate.type())) score += 10.0;
            if (candidate.signals().contains("NOUN_COMBINATION")) score += 8.0;
            if (candidate.signals().contains("SEO_MODIFIER")) score += 7.0;
            if (extraction.rakePhrases().stream().anyMatch(candidate.keyword()::contains)) score += 7.0;
            score += Math.min(10.0, extraction.frequencyFor(candidate.keyword()) * 2.0);
            score += naturalnessScore * 0.25;

            score = clamp(round(score), 0.0, 100.0);
            RankedKeyword ranked = new RankedKeyword(candidate, score, naturalnessScore);
            rankedByKeyword.merge(candidate.keyword(), ranked, (existing, incoming) ->
                    incoming.score() > existing.score() ? incoming : existing);
        }

        return rankedByKeyword.values().stream()
                .sorted(Comparator.comparingDouble(RankedKeyword::score).reversed())
                .limit(maxKeywords * 3L)
                .toList();
    }

    public double phraseNaturalnessScore(String keyword) {
        if (keyword == null || keyword.isBlank()) return 0.0;

        String[] words = keyword.toLowerCase(Locale.ROOT).trim().split("\\s+");
        double score = 60.0;
        int count = words.length;

        if (count >= 3 && count <= 6) score += 22.0;
        if (count == 2) score += 6.0;
        if (count == 1) score -= 32.0;
        if (count > 6) score -= 28.0;

        long stopWords = Arrays.stream(words).filter(STOP_WORDS::contains).count();
        double stopwordRatio = (double) stopWords / count;
        if (stopwordRatio > 0.5) score -= 30.0;
        else if (stopwordRatio > 0.35) score -= 10.0;

        if (hasRepeatedToken(words)) score -= 45.0;
        if (looksBroken(words)) score -= 35.0;
        if (hasIntentPattern(keyword)) score += 14.0;
        if (containsSeoModifier(keyword)) score += 10.0;
        if (containsKnownTech(keyword)) score += 8.0;

        return clamp(round(score), 0.0, 100.0);
    }

    public boolean isInvalidPhrase(String keyword) {
        return phraseNaturalnessScore(keyword) < 40.0;
    }

    private boolean hasRepeatedToken(String[] words) {
        for (int i = 1; i < words.length; i++) {
            if (words[i].equals(words[i - 1])) return true;
        }
        return Set.of(words).size() != words.length;
    }

    private boolean looksBroken(String[] words) {
        if (words.length == 0) return true;
        String first = words[0];
        String last = words[words.length - 1];
        if (Set.of("to", "for", "with", "and", "or", "by", "from", "of").contains(first)) return true;
        if (Set.of("to", "for", "with", "and", "or", "by", "from", "of", "how", "best").contains(last)) return true;
        return Arrays.stream(words).anyMatch(word -> word.length() == 1 && !"c".equals(word));
    }

    private boolean hasIntentPattern(String keyword) {
        String lower = keyword.toLowerCase(Locale.ROOT);
        return lower.startsWith("how to ")
                || lower.contains(" tutorial")
                || lower.startsWith("beginner guide")
                || lower.contains(" explained")
                || lower.contains(" vs ")
                || lower.contains(" for beginners");
    }

    private boolean containsSeoModifier(String keyword) {
        String lower = keyword.toLowerCase(Locale.ROOT);
        return List.of("best", "tutorial", "guide", "learn", "setup", "explained", "beginner", "optimization", "practices")
                .stream().anyMatch(lower::contains);
    }

    private boolean containsKnownTech(String keyword) {
        String lower = keyword.toLowerCase(Locale.ROOT);
        return List.of("react", "vue", "angular", "spring", "boot", "redis", "java", "javascript", "typescript",
                "python", "node", "mysql", "docker", "api", "vite", "next", "seo").stream().anyMatch(lower::contains);
    }

    private int wordCount(String keyword) {
        if (keyword == null || keyword.isBlank()) return 0;
        return keyword.trim().split("\\s+").length;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public record RankedKeyword(
            KeywordOptimizationService.KeywordCandidate candidate,
            double score,
            double naturalnessScore
    ) {
    }
}
