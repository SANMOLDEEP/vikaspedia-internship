package com.seo.keywordgenerator.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class KeywordExtractor {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is", "it",
            "no", "not", "of", "on", "or", "such", "that", "the", "their", "then", "there", "these", "they",
            "this", "to", "was", "will", "with", "i", "you", "your", "we", "our", "us", "can", "have", "has",
            "had", "what", "when", "where", "who", "which", "why", "how", "all", "any", "both", "each", "few",
            "more", "most", "other", "some", "such", "only", "own", "same", "so", "than", "too", "very",
            "just", "now", "also", "get", "got", "go", "went", "come", "came", "see", "saw", "know", "knew",
            "take", "took", "make", "made", "do", "did", "use", "used", "from", "up", "out", "about", "over",
            "again", "further", "once", "here", "there", "why", "how", "all", "any", "both", "each", "few",
            "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than",
            "too", "very", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
            "hundred", "thousand", "million", "billion", "first", "second", "third", "last", "next", "previous"
    ));

    private static final Pattern WORD_PATTERN = Pattern.compile("\\b[a-zA-Z]{3,}\\b");
    private static final Pattern PHRASE_PATTERN = Pattern.compile("\\b[a-zA-Z]+(?:\\s+[a-zA-Z]+){1,2}\\b");

    public List<String> extractKeywords(String content) {
        log.debug("Extracting keywords from content: {}", content);
        
        if (content == null || content.trim().isEmpty()) {
            log.debug("Content is null or empty, returning empty list");
            return Collections.emptyList();
        }

        String cleanedContent = content.toLowerCase()
                .replaceAll("[^a-zA-Z\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        log.debug("Cleaned content: {}", cleanedContent);

        Map<String, Double> wordScores = calculateWordScores(cleanedContent);
        log.debug("Word scores calculated: {}", wordScores);
        
        List<String> singleWords = extractSingleWords(wordScores);
        log.debug("Single words extracted: {}", singleWords);
        
        List<String> phrases = extractPhrases(cleanedContent, wordScores);
        log.debug("Phrases extracted: {}", phrases);

        List<String> allKeywords = new ArrayList<>();
        allKeywords.addAll(singleWords);
        allKeywords.addAll(phrases);

        // Clean and validate keywords
        List<String> cleanedKeywords = allKeywords.stream()
                .map(this::cleanKeyword)
                .filter(this::isValidKeyword)
                .distinct()
                .sorted((a, b) -> Double.compare(
                        wordScores.getOrDefault(b, 0.0),
                        wordScores.getOrDefault(a, 0.0)
                ))
                .limit(30)
                .collect(Collectors.toList());
        
        log.debug("Cleaned keywords: {}", cleanedKeywords);
        return cleanedKeywords;
    }

    private Map<String, Double> calculateWordScores(String content) {
        Map<String, Integer> wordFrequencies = new HashMap<>();
        String[] words = content.split("\\s+");
        int totalWords = words.length;

        for (String word : words) {
            if (!STOP_WORDS.contains(word) && word.length() >= 3) {
                wordFrequencies.put(word, wordFrequencies.getOrDefault(word, 0) + 1);
            }
        }

        Map<String, Double> wordScores = new HashMap<>();
        for (Map.Entry<String, Integer> entry : wordFrequencies.entrySet()) {
            double frequency = entry.getValue();
            double normalizedFrequency = frequency / totalWords;
            double lengthBonus = Math.min(entry.getKey().length() / 10.0, 0.3);
            double score = normalizedFrequency * 100 + lengthBonus * 10;
            wordScores.put(entry.getKey(), score);
        }

        return wordScores;
    }

    private List<String> extractSingleWords(Map<String, Double> wordScores) {
        return wordScores.entrySet().stream()
                .filter(entry -> entry.getValue() > 0.1) // Lower threshold for short content
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(15)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<String> extractPhrases(String content, Map<String, Double> wordScores) {
        List<String> phrases = new ArrayList<>();
        String[] words = content.split("\\s+");

        for (int i = 0; i < words.length - 1; i++) {
            String word1 = words[i];
            String word2 = words[i + 1];

            if (!STOP_WORDS.contains(word1) && !STOP_WORDS.contains(word2) &&
                wordScores.containsKey(word1) && wordScores.containsKey(word2)) {
                
                String phrase = word1 + " " + word2;
                double phraseScore = (wordScores.get(word1) + wordScores.get(word2)) / 2;
                
                if (phraseScore > 0.5) { // Lower threshold for short content
                    phrases.add(phrase);
                }
            }
        }

        for (int i = 0; i < words.length - 2; i++) {
            String word1 = words[i];
            String word2 = words[i + 1];
            String word3 = words[i + 2];

            if (!STOP_WORDS.contains(word1) && !STOP_WORDS.contains(word2) && !STOP_WORDS.contains(word3) &&
                wordScores.containsKey(word1) && wordScores.containsKey(word2) && wordScores.containsKey(word3)) {
                
                String phrase = word1 + " " + word2 + " " + word3;
                double phraseScore = (wordScores.get(word1) + wordScores.get(word2) + wordScores.get(word3)) / 3;
                
                if (phraseScore > 1.0) { // Lower threshold for short content
                    phrases.add(phrase);
                }
            }
        }

        return phrases.stream()
                .distinct()
                .sorted(Comparator.comparingDouble(phrase -> 
                    Arrays.stream(((String) phrase).split("\\s+"))
                          .mapToDouble(word -> wordScores.getOrDefault(word, 0.0))
                          .average()
                          .orElse(0.0)
                ).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * Clean keyword by removing duplicate words and fixing formatting
     */
    private String cleanKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return keyword;
        }
        
        // Convert to lowercase and split into words
        String[] words = keyword.toLowerCase().trim().split("\\s+");
        
        // Remove duplicate words while preserving order
        Set<String> seenWords = new HashSet<>();
        List<String> uniqueWords = new ArrayList<>();
        
        for (String word : words) {
            if (!seenWords.contains(word) && !word.isEmpty()) {
                seenWords.add(word);
                uniqueWords.add(word);
            }
        }
        
        // Rejoin the words
        return String.join(" ", uniqueWords);
    }

    /**
     * Validate keyword quality rules
     */
    private boolean isValidKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = keyword.trim();
        String[] words = trimmed.split("\\s+");
        
        // Rule 1: Minimum word count (at least 1 word)
        if (words.length < 1) {
            log.debug("Rejected keyword '{}': too few words", keyword);
            return false;
        }
        
        // Rule 2: Maximum word count (reasonable limit)
        if (words.length > 4) {
            log.debug("Rejected keyword '{}': too many words", keyword);
            return false;
        }
        
        // Rule 3: Minimum keyword length
        if (trimmed.length() < 3) {
            log.debug("Rejected keyword '{}': too short", keyword);
            return false;
        }
        
        // Rule 4: Maximum keyword length
        if (trimmed.length() > 100) {
            log.debug("Rejected keyword '{}': too long", keyword);
            return false;
        }
        
        // Rule 5: No repeated words (already handled by cleaning, but double-check)
        Set<String> wordSet = new HashSet<>(Arrays.asList(words));
        if (wordSet.size() != words.length) {
            log.debug("Rejected keyword '{}': contains duplicate words", keyword);
            return false;
        }
        
        // Rule 6: No meaningless patterns
        if (containsMeaninglessPattern(trimmed)) {
            log.debug("Rejected keyword '{}': contains meaningless pattern", keyword);
            return false;
        }
        
        // Rule 7: At least one meaningful word (not all stop words)
        boolean hasMeaningfulWord = Arrays.stream(words)
                .anyMatch(word -> !STOP_WORDS.contains(word) && word.length() >= 3);
        
        if (!hasMeaningfulWord) {
            log.debug("Rejected keyword '{}': no meaningful words", keyword);
            return false;
        }
        
        return true;
    }

    /**
     * Check for meaningless patterns in keywords
     */
    private boolean containsMeaninglessPattern(String keyword) {
        // Pattern 1: Repeated words (should be caught by cleaning, but safety check)
        if (keyword.matches(".*\\b(\\w+)\\s+\\1\\b.*")) {
            return true;
        }
        
        // Pattern 2: Common meaningless combinations
        String[] meaninglessCombinations = {
            "what to", "how to", "where to", "when to", "why to",
            "can i", "could i", "should i", "would i",
            "i want", "i need", "i like", "i love",
            "best way", "good way", "easy way",
            "step by", "step step", "guide guide"
        };
        
        for (String pattern : meaninglessCombinations) {
            if (keyword.contains(pattern)) {
                return true;
            }
        }
        
        // Pattern 3: Too many short words (less than 3 characters each)
        String[] words = keyword.split("\\s+");
        long shortWordCount = Arrays.stream(words)
                .filter(word -> word.length() < 3)
                .count();
        
        if (shortWordCount > words.length / 2) {
            return true;
        }
        
        return false;
    }
}
