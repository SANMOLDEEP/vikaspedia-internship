package com.seo.keywordgenerator.service;

import com.seo.keywordgenerator.scheduler.TrendingKeywordScheduler;
import com.seo.keywordgenerator.util.KeywordExtractor;
import com.seo.keywordgenerator.util.RAKEExtractor;
import com.seo.keywordgenerator.util.TextPreprocessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeywordExtractionService {

    private final TextPreprocessor textPreprocessor;
    private final KeywordExtractor keywordExtractor;
    private final RAKEExtractor rakeExtractor;
    private final TrendingKeywordScheduler trendingScheduler;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "from", "in", "into",
            "is", "it", "of", "on", "or", "that", "the", "their", "then", "there", "these", "they",
            "this", "to", "was", "will", "with", "using", "used", "use", "can", "have", "has", "had",
            "its", "your", "our", "we", "you", "them", "itself", "designed", "provides", "allows"
    );

    public ExtractionResult extract(String originalContent) {
        if (originalContent == null || originalContent.isBlank()) {
            return ExtractionResult.empty();
        }

        String preprocessedContent = preprocess(originalContent);
        Map<String, Integer> internalFrequency = calculateFrequency(originalContent);
        Set<String> contentTokens = internalFrequency.keySet();

        CompletableFuture<List<String>> keywordFuture = CompletableFuture.supplyAsync(
                () -> safeKeywordExtraction(preprocessedContent), executorService);
        CompletableFuture<List<String>> rakeFuture = CompletableFuture.supplyAsync(
                () -> safeRakeExtraction(originalContent), executorService);
        CompletableFuture<List<String>> nounPhraseFuture = CompletableFuture.supplyAsync(
                () -> extractNounPhrases(originalContent), executorService);
        CompletableFuture<List<String>> trendingFuture = CompletableFuture.supplyAsync(
                () -> safeTrendingKeywords(contentTokens), executorService);

        CompletableFuture.allOf(keywordFuture, rakeFuture, nounPhraseFuture, trendingFuture).join();

        List<String> rakePhrases = joinFuture(rakeFuture);
        List<String> nounPhrases = joinFuture(nounPhraseFuture);
        Set<String> rawKeywords = new LinkedHashSet<>();
        rawKeywords.addAll(joinFuture(keywordFuture));
        rawKeywords.addAll(rakePhrases);
        rawKeywords.addAll(nounPhrases);
        rawKeywords.addAll(joinFuture(trendingFuture));

        List<String> topics = selectTopics(rawKeywords, rakePhrases, nounPhrases, internalFrequency);
        return new ExtractionResult(rawKeywords, rakePhrases, nounPhrases, topics, internalFrequency);
    }

    private String preprocess(String content) {
        try {
            return textPreprocessor.preprocessText(content);
        } catch (Exception e) {
            log.warn("Text preprocessing failed, using original content: {}", e.getMessage());
            return content;
        }
    }

    private List<String> safeKeywordExtraction(String content) {
        try {
            return keywordExtractor.extractKeywords(content).stream()
                    .map(this::normalizePhrase)
                    .filter(this::isUsableTopic)
                    .toList();
        } catch (Exception e) {
            log.warn("Keyword extractor failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> safeRakeExtraction(String content) {
        try {
            return rakeExtractor.extractKeywords(content).stream()
                    .sorted(Comparator.comparingDouble(RAKEExtractor.RAKEScore::getScore).reversed())
                    .map(RAKEExtractor.RAKEScore::getPhrase)
                    .map(this::normalizePhrase)
                    .filter(this::isUsableTopic)
                    .limit(12)
                    .toList();
        } catch (Exception e) {
            log.warn("RAKE extraction failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> safeTrendingKeywords(Set<String> contentTokens) {
        try {
            List<TrendingKeywordScheduler.TrendingKeyword> cached = trendingScheduler.getCachedTrendingKeywords();
            if (cached == null || cached.isEmpty()) {
                return Collections.emptyList();
            }

            return cached.stream()
                    .map(TrendingKeywordScheduler.TrendingKeyword::getKeyword)
                    .map(this::normalizePhrase)
                    .filter(keyword -> overlapsContent(keyword, contentTokens))
                    .filter(this::isUsableTopic)
                    .limit(10)
                    .toList();
        } catch (Exception e) {
            log.warn("Cached trending keyword read failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> extractNounPhrases(String content) {
        List<String> tokens = tokenize(content);
        List<String> phrases = new ArrayList<>();

        for (int size = 3; size >= 1; size--) {
            for (int i = 0; i <= tokens.size() - size; i++) {
                List<String> window = tokens.subList(i, i + size);
                if (window.stream().allMatch(token -> !STOP_WORDS.contains(token))) {
                    String phrase = String.join(" ", window);
                    if (isUsableTopic(phrase)) {
                        phrases.add(phrase);
                    }
                }
            }
        }

        return phrases.stream().distinct().limit(24).toList();
    }

    private List<String> selectTopics(Set<String> rawKeywords, List<String> rakePhrases, List<String> nounPhrases,
                                      Map<String, Integer> internalFrequency) {
        Set<String> topicPool = new LinkedHashSet<>();
        topicPool.addAll(rakePhrases);
        topicPool.addAll(nounPhrases);
        topicPool.addAll(rawKeywords);

        List<String> topics = topicPool.stream()
                .map(this::normalizePhrase)
                .filter(this::isUsableTopic)
                .sorted(Comparator.comparingDouble((String topic) -> topicScore(topic, rakePhrases, nounPhrases, internalFrequency)).reversed())
                .limit(10)
                .toList();

        if (!topics.isEmpty()) {
            return topics;
        }

        return internalFrequency.keySet().stream()
                .filter(token -> token.length() >= 3)
                .limit(5)
                .toList();
    }

    private double topicScore(String topic, List<String> rakePhrases, List<String> nounPhrases, Map<String, Integer> frequency) {
        int wordCount = wordCount(topic);
        double score = wordCount == 1 ? 40.0 : wordCount == 2 ? 70.0 : 82.0;
        if (rakePhrases.contains(topic)) score += 18.0;
        if (nounPhrases.contains(topic)) score += 12.0;
        score += Arrays.stream(topic.split("\\s+")).mapToInt(token -> frequency.getOrDefault(token, 0)).sum() * 4.0;
        if (containsKnownTech(topic)) score += 12.0;
        return score;
    }

    private Map<String, Integer> calculateFrequency(String content) {
        Map<String, Integer> frequency = new HashMap<>();
        for (String token : tokenize(content)) {
            if (!STOP_WORDS.contains(token)) {
                frequency.merge(token, 1, Integer::sum);
            }
        }
        return frequency;
    }

    private List<String> tokenize(String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(content.toLowerCase(Locale.ROOT)
                        .replace("vue.js", "vue js")
                        .replace("next.js", "next js")
                        .replace("node.js", "node js")
                        .replaceAll("[^a-z0-9+#\\s]", " ")
                        .split("\\s+"))
                .map(String::trim)
                .filter(token -> token.length() >= 2)
                .toList();
    }

    public String normalizePhrase(String phrase) {
        if (phrase == null) {
            return "";
        }

        List<String> tokens = tokenize(phrase);
        Set<String> seen = new HashSet<>();
        List<String> deduped = new ArrayList<>();
        for (String token : tokens) {
            if (seen.add(token)) {
                deduped.add(token);
            }
        }
        return String.join(" ", deduped);
    }

    private boolean isUsableTopic(String phrase) {
        if (phrase == null || phrase.isBlank()) return false;
        String normalized = normalizePhrase(phrase);
        int wordCount = wordCount(normalized);
        if (wordCount < 1 || wordCount > 4) return false;
        if (normalized.length() < 2 || normalized.length() > 55) return false;
        List<String> words = Arrays.asList(normalized.split("\\s+"));
        if (words.stream().allMatch(STOP_WORDS::contains)) return false;
        if (STOP_WORDS.contains(words.get(0)) || STOP_WORDS.contains(words.get(words.size() - 1))) return false;
        return words.stream().anyMatch(word -> word.length() >= 3 || containsKnownTech(word));
    }

    private boolean overlapsContent(String keyword, Set<String> contentTokens) {
        if (contentTokens == null || contentTokens.isEmpty()) return false;
        return Arrays.stream(keyword.split("\\s+")).anyMatch(contentTokens::contains);
    }

    private boolean containsKnownTech(String phrase) {
        String lower = phrase.toLowerCase(Locale.ROOT);
        return List.of("react", "vue", "angular", "spring", "boot", "redis", "java", "javascript", "typescript",
                "python", "node", "mysql", "docker", "api", "vite", "next", "seo").stream().anyMatch(lower::contains);
    }

    private int wordCount(String phrase) {
        if (phrase == null || phrase.isBlank()) return 0;
        return phrase.trim().split("\\s+").length;
    }

    private List<String> joinFuture(CompletableFuture<List<String>> future) {
        try {
            return future.get();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public record ExtractionResult(
            Set<String> rawKeywords,
            List<String> rakePhrases,
            List<String> nounPhrases,
            List<String> topics,
            Map<String, Integer> internalFrequency
    ) {
        static ExtractionResult empty() {
            return new ExtractionResult(Set.of(), List.of(), List.of(), List.of(), Map.of());
        }

        public int frequencyFor(String keyword) {
            if (keyword == null || keyword.isBlank()) return 0;
            return Arrays.stream(keyword.split("\\s+"))
                    .mapToInt(token -> internalFrequency.getOrDefault(token, 0))
                    .sum();
        }
    }
}
