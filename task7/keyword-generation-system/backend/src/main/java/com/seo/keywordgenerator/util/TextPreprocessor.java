package com.seo.keywordgenerator.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TextPreprocessor {

    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[^a-zA-Z0-9\\s]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern WORD_PATTERN = Pattern.compile("\\b[a-zA-Z0-9]+\\b");
    
    private Set<String> stopwords;
    private final Map<String, List<String>> tokenizationCache = new ConcurrentHashMap<>();
    
    public TextPreprocessor() {
        loadStopwords();
    }
    
    private void loadStopwords() {
        try {
            ClassPathResource resource = new ClassPathResource("stopwords.txt");
            Set<String> loadedStopwords = new BufferedReader(new InputStreamReader(resource.getInputStream()))
                    .lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            
            this.stopwords = Collections.unmodifiableSet(loadedStopwords);
            log.info("Loaded {} stopwords from stopwords.txt", this.stopwords.size());
            
        } catch (IOException e) {
            log.error("Failed to load stopwords from stopwords.txt, using default stopwords", e);
            this.stopwords = getDefaultStopwords();
        }
    }
    
    private Set<String> getDefaultStopwords() {
        return Set.of(
                "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is", "it",
                "no", "not", "of", "on", "or", "such", "that", "the", "their", "then", "there", "these", "they",
                "this", "to", "was", "will", "with", "i", "you", "your", "we", "our", "us", "can", "have", "has",
                "had", "what", "when", "where", "who", "which", "why", "how", "all", "any", "both", "each", "few",
                "more", "most", "other", "some", "such", "only", "own", "same", "so", "than", "too", "very"
        );
    }
    
    public String preprocessText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        
        String processed = text;
        
        processed = convertToLowerCase(processed);
        processed = removePunctuation(processed);
        processed = normalizeWhitespace(processed);
        
        return processed.trim();
    }
    
    public List<String> tokenize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String cacheKey = text.hashCode() + "";
        return tokenizationCache.computeIfAbsent(cacheKey, k -> performTokenization(text));
    }
    
    private List<String> performTokenization(String text) {
        String preprocessed = preprocessText(text);
        
        if (preprocessed.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> tokens = Arrays.asList(preprocessed.split("\\s+"));
        
        return tokens.stream()
                .filter(token -> !token.isEmpty())
                .filter(token -> token.length() >= 2)
                .collect(Collectors.toList());
    }
    
    public List<String> removeStopwords(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyList();
        }
        
        return tokens.stream()
                .filter(token -> !stopwords.contains(token.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    public List<String> removeStopwordsAndFilter(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyList();
        }
        
        return tokens.stream()
                .filter(token -> !token.isEmpty())
                .filter(token -> token.length() >= 2)
                .filter(token -> !stopwords.contains(token.toLowerCase()))
                .filter(token -> token.matches("[a-zA-Z0-9]+"))
                .collect(Collectors.toList());
    }
    
    public String cleanAndTokenizeToString(String text) {
        List<String> tokens = tokenize(text);
        List<String> filteredTokens = removeStopwordsAndFilter(tokens);
        return String.join(" ", filteredTokens);
    }
    
    public List<String> getCleanTokens(String text) {
        List<String> tokens = tokenize(text);
        return removeStopwordsAndFilter(tokens);
    }
    
    private String convertToLowerCase(String text) {
        return text.toLowerCase();
    }
    
    private String removePunctuation(String text) {
        return PUNCTUATION_PATTERN.matcher(text).replaceAll(" ");
    }
    
    private String normalizeWhitespace(String text) {
        return WHITESPACE_PATTERN.matcher(text).replaceAll(" ");
    }
    
    @Cacheable(value = "stopwords", key = "'all'")
    public Set<String> getStopwords() {
        return new HashSet<>(stopwords);
    }
    
    public boolean isStopword(String word) {
        return word != null && stopwords.contains(word.toLowerCase());
    }
    
    public Map<String, Integer> getTokenFrequency(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return tokens.stream()
                .filter(Objects::nonNull)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toMap(
                        token -> token.toLowerCase(),
                        token -> 1,
                        Integer::sum
                ));
    }
    
    public Map<String, Integer> getCleanTokenFrequency(String text) {
        List<String> cleanTokens = getCleanTokens(text);
        return getTokenFrequency(cleanTokens);
    }
    
    public String extractSentences(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        
        String cleaned = preprocessText(text);
        String[] sentences = cleaned.split("(?<=[.!?])\\s+");
        
        return Arrays.stream(sentences)
                .filter(sentence -> !sentence.trim().isEmpty())
                .collect(Collectors.joining(" "));
    }
    
    public int getWordCount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        
        String preprocessed = preprocessText(text);
        if (preprocessed.isEmpty()) {
            return 0;
        }
        
        return preprocessed.split("\\s+").length;
    }
    
    public void clearCache() {
        tokenizationCache.clear();
        log.info("TextPreprocessor cache cleared");
    }
    
    public int getCacheSize() {
        return tokenizationCache.size();
    }
}
