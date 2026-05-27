package com.seo.keywordgenerator.service;

import com.seo.keywordgenerator.util.KeywordGeneratorEngineNew;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class KeywordOptimizationService {

    private final KeywordGeneratorEngineNew keywordGeneratorEngineNew;
    private final KeywordExtractionService keywordExtractionService;

    public List<KeywordCandidate> generateCandidates(KeywordExtractionService.ExtractionResult extraction,
                                                     String originalContent,
                                                     int maxCandidates) {
        if (extraction == null || extraction.topics().isEmpty()) {
            return List.of();
        }

        Map<String, KeywordCandidate> candidates = new LinkedHashMap<>();
        List<String> topics = extraction.topics();

        addEngineCandidates(candidates, extraction, originalContent, maxCandidates);
        addIntentPatternCandidates(candidates, topics, extraction, maxCandidates);

        return candidates.values().stream()
                .filter(candidate -> candidate.qualityScore() >= 45.0)
                .limit(Math.max(maxCandidates, 20))
                .toList();
    }

    private void addEngineCandidates(Map<String, KeywordCandidate> candidates,
                                     KeywordExtractionService.ExtractionResult extraction,
                                     String originalContent,
                                     int maxCandidates) {
        try {
            keywordGeneratorEngineNew.generateKeywords(new ArrayList<>(extraction.rawKeywords()), maxCandidates, originalContent)
                    .forEach(generated -> addCandidate(candidates,
                            generated.getKeyword(),
                            inferIntent(generated.getKeyword()),
                            generated.getType(),
                            generated.getScore() * 0.75,
                            Set.of("RAKE_TEMPLATE")));
        } catch (Exception ignored) {
            // The new pattern layer below is the primary generator; the legacy engine is a bonus signal.
        }
    }

    private void addIntentPatternCandidates(Map<String, KeywordCandidate> candidates,
                                            List<String> topics,
                                            KeywordExtractionService.ExtractionResult extraction,
                                            int maxCandidates) {
        int limit = Math.min(topics.size(), 8);
        for (int i = 0; i < limit; i++) {
            String topic = topics.get(i);
            addCandidate(candidates, "how to learn " + topic, "INFORMATIONAL", "LONG_TAIL", 86.0, Set.of("INTENT_PATTERN"));
            addCandidate(candidates, "how to use " + topic, "TUTORIAL", "LONG_TAIL", 84.0, Set.of("INTENT_PATTERN"));
            addCandidate(candidates, topic + " tutorial", "TUTORIAL", "LONG_TAIL", 83.0, Set.of("SEO_MODIFIER"));
            addCandidate(candidates, topic + " tutorial for beginners", "BEGINNER_FOCUSED", "LONG_TAIL", 91.0, Set.of("SEO_MODIFIER"));
            addCandidate(candidates, "beginner guide to " + topic, "BEGINNER_FOCUSED", "LONG_TAIL", 89.0, Set.of("SEO_MODIFIER"));
            addCandidate(candidates, topic + " explained", "INFORMATIONAL", "LONG_TAIL", 78.0, Set.of("INTENT_PATTERN"));
            addCandidate(candidates, "best " + topic + " guide", "INFORMATIONAL", "LONG_TAIL", 80.0, Set.of("SEO_MODIFIER"));
            addCandidate(candidates, topic + " best practices", "OPTIMIZATION_FOCUSED", "LONG_TAIL", 84.0, Set.of("SEO_MODIFIER"));
            addCandidate(candidates, topic + " optimization guide", "OPTIMIZATION_FOCUSED", "LONG_TAIL", 82.0, Set.of("INTENT_PATTERN"));
            addCandidate(candidates, topic + " setup tutorial", "TUTORIAL", "LONG_TAIL", 81.0, Set.of("SEO_MODIFIER"));

            String supportTopic = pickSupportTopic(topic, topics, extraction);
            if (supportTopic != null) {
                addCandidate(candidates, "best " + topic + " for " + supportTopic, "COMPARISON", "LONG_TAIL", 86.0, Set.of("NOUN_COMBINATION"));
                addCandidate(candidates, topic + " vs " + supportTopic, "COMPARISON", "LONG_TAIL", 79.0, Set.of("NOUN_COMBINATION"));
                addCandidate(candidates, topic + " with " + supportTopic, "TUTORIAL", "LONG_TAIL", 76.0, Set.of("NOUN_COMBINATION"));
            }

            if (candidates.size() >= maxCandidates * 3) {
                break;
            }
        }
    }

    private String pickSupportTopic(String topic, List<String> topics, KeywordExtractionService.ExtractionResult extraction) {
        for (String candidate : topics) {
            if (!Objects.equals(candidate, topic) && !sharesAllImportantWords(topic, candidate)) {
                return candidate;
            }
        }

        return extraction.internalFrequency().keySet().stream()
                .filter(token -> !topic.contains(token))
                .filter(token -> token.length() >= 3)
                .findFirst()
                .orElse(null);
    }

    private boolean sharesAllImportantWords(String left, String right) {
        Set<String> leftWords = new LinkedHashSet<>(Arrays.asList(left.split("\\s+")));
        Set<String> rightWords = new LinkedHashSet<>(Arrays.asList(right.split("\\s+")));
        return leftWords.containsAll(rightWords) || rightWords.containsAll(leftWords);
    }

    private void addCandidate(Map<String, KeywordCandidate> candidates, String keyword, String intent, String type,
                              double qualityScore, Set<String> signals) {
        String cleaned = keywordExtractionService.normalizePhrase(keyword);
        if (!isValidCandidate(cleaned)) {
            return;
        }

        KeywordCandidate candidate = new KeywordCandidate(cleaned, intent, determineType(cleaned, type), qualityScore, signals);
        candidates.merge(cleaned, candidate, (existing, incoming) -> existing.merge(incoming));
    }

    private boolean isValidCandidate(String keyword) {
        if (keyword == null || keyword.isBlank()) return false;
        String[] words = keyword.split("\\s+");
        if (words.length < 2 || words.length > 6) return false;
        if (new LinkedHashSet<>(Arrays.asList(words)).size() != words.length) return false;
        long shortWords = Arrays.stream(words).filter(word -> word.length() <= 2).count();
        if (shortWords > words.length / 2) return false;
        return Arrays.stream(words).anyMatch(word -> word.length() >= 3);
    }

    private String determineType(String keyword, String fallback) {
        int words = keyword.split("\\s+").length;
        if (words >= 3) return "LONG_TAIL";
        if (words == 2) return "MEDIUM";
        return fallback == null || fallback.isBlank() ? "SHORT" : fallback;
    }

    private String inferIntent(String keyword) {
        String lower = keyword.toLowerCase(Locale.ROOT);
        if (lower.contains(" vs ") || lower.startsWith("best ") && lower.contains(" for ")) return "COMPARISON";
        if (lower.contains("beginner")) return "BEGINNER_FOCUSED";
        if (lower.contains("tutorial") || lower.startsWith("how to use") || lower.contains("setup")) return "TUTORIAL";
        if (lower.contains("best practices") || lower.contains("optimization") || lower.contains("performance")) return "OPTIMIZATION_FOCUSED";
        return "INFORMATIONAL";
    }

    public record KeywordCandidate(
            String keyword,
            String searchIntent,
            String type,
            double qualityScore,
            Set<String> signals
    ) {
        KeywordCandidate merge(KeywordCandidate other) {
            Set<String> mergedSignals = new LinkedHashSet<>(signals);
            mergedSignals.addAll(other.signals);
            return new KeywordCandidate(keyword, searchIntent, type, Math.max(qualityScore, other.qualityScore), mergedSignals);
        }
    }
}
