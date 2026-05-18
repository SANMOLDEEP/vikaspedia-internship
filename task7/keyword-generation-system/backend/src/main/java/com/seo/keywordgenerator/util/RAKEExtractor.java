package com.seo.keywordgenerator.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RAKEExtractor {

    private final TextPreprocessor textPreprocessor;
    
    private static final Pattern SENTENCE_DELIMITERS = Pattern.compile("[.!?]+");
    private static final Pattern PHRASE_DELIMITERS = Pattern.compile("[:;,\\-\\n]");
    
    public RAKEExtractor(TextPreprocessor textPreprocessor) {
        this.textPreprocessor = textPreprocessor;
    }
    
    public List<RAKEScore> extractKeywords(String text) {
        return extractKeywords(text, 0);
    }
    
    public List<RAKEScore> extractKeywords(String text, int topN) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> sentences = splitIntoSentences(text);
        List<String> candidatePhrases = extractCandidatePhrases(sentences);
        
        if (candidatePhrases.isEmpty()) {
            return Collections.emptyList();
        }
        
        Map<String, Integer> wordFrequencies = calculateWordFrequencies(candidatePhrases);
        Map<String, Integer> wordDegrees = calculateWordDegrees(candidatePhrases, wordFrequencies);
        Map<String, Double> wordScores = calculateWordScores(wordFrequencies, wordDegrees);
        
        Map<String, Double> phraseScores = calculatePhraseScores(candidatePhrases, wordScores);
        
        List<RAKEScore> rankedPhrases = rankPhrases(phraseScores);
        
        if (topN > 0) {
            return rankedPhrases.stream().limit(topN).collect(Collectors.toList());
        }
        
        return rankedPhrases;
    }
    
    public List<String> extractKeywordPhrases(String text) {
        return extractKeywordPhrases(text, 0);
    }
    
    public List<String> extractKeywordPhrases(String text, int topN) {
        List<RAKEScore> scores = extractKeywords(text, topN);
        return scores.stream()
                .map(RAKEScore::getPhrase)
                .collect(Collectors.toList());
    }
    
    private List<String> splitIntoSentences(String text) {
        String[] sentences = SENTENCE_DELIMITERS.split(text);
        return Arrays.stream(sentences)
                .map(String::trim)
                .filter(sentence -> !sentence.isEmpty())
                .collect(Collectors.toList());
    }
    
    private List<String> extractCandidatePhrases(List<String> sentences) {
        List<String> candidatePhrases = new ArrayList<>();
        Set<String> stopwords = textPreprocessor.getStopwords();
        
        for (String sentence : sentences) {
            // Don't preprocess individual sentences - work with original text
            String[] phrases = PHRASE_DELIMITERS.split(sentence);
            
            for (String phrase : phrases) {
                phrase = phrase.trim();
                if (!phrase.isEmpty()) {
                    List<String> words = Arrays.asList(phrase.toLowerCase().split("\\s+"));
                    List<String> filteredWords = words.stream()
                            .filter(word -> !word.isEmpty())
                            .filter(word -> word.length() >= 2)
                            .filter(word -> !stopwords.contains(word))
                            .collect(Collectors.toList());
                    
                    if (filteredWords.size() >= 1 && filteredWords.size() <= 4) {
                        String candidatePhrase = String.join(" ", filteredWords);
                        candidatePhrases.add(candidatePhrase);
                    }
                }
            }
        }
        
        return candidatePhrases;
    }
    
    private Map<String, Integer> calculateWordFrequencies(List<String> candidatePhrases) {
        Map<String, Integer> frequencies = new HashMap<>();
        
        for (String phrase : candidatePhrases) {
            String[] words = phrase.split("\\s+");
            for (String word : words) {
                frequencies.put(word, frequencies.getOrDefault(word, 0) + 1);
            }
        }
        
        return frequencies;
    }
    
    private Map<String, Integer> calculateWordDegrees(List<String> candidatePhrases, Map<String, Integer> wordFrequencies) {
        Map<String, Integer> degrees = new HashMap<>();
        
        for (String phrase : candidatePhrases) {
            String[] words = phrase.split("\\s+");
            int phraseLength = words.length;
            
            for (String word : words) {
                int degree = phraseLength - 1;
                degrees.put(word, degrees.getOrDefault(word, 0) + degree);
            }
        }
        
        for (String word : wordFrequencies.keySet()) {
            degrees.put(word, degrees.getOrDefault(word, 0) + wordFrequencies.get(word));
        }
        
        return degrees;
    }
    
    private Map<String, Double> calculateWordScores(Map<String, Integer> wordFrequencies, Map<String, Integer> wordDegrees) {
        Map<String, Double> wordScores = new HashMap<>();
        
        for (String word : wordFrequencies.keySet()) {
            int frequency = wordFrequencies.get(word);
            int degree = wordDegrees.get(word);
            double score = (double) degree / frequency;
            wordScores.put(word, score);
        }
        
        return wordScores;
    }
    
    private Map<String, Double> calculatePhraseScores(List<String> candidatePhrases, Map<String, Double> wordScores) {
        Map<String, Double> phraseScores = new HashMap<>();
        
        for (String phrase : candidatePhrases) {
            String[] words = phrase.split("\\s+");
            double totalScore = 0.0;
            
            for (String word : words) {
                totalScore += wordScores.getOrDefault(word, 0.0);
            }
            
            phraseScores.put(phrase, totalScore);
        }
        
        return phraseScores;
    }
    
    private List<RAKEScore> rankPhrases(Map<String, Double> phraseScores) {
        return phraseScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> new RAKEScore(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
    
    public List<RAKEScore> extractKeywordsWithLengthFilter(String text, int minLength, int maxLength) {
        List<RAKEScore> allScores = extractKeywords(text);
        
        return allScores.stream()
                .filter(score -> {
                    int wordCount = score.getPhrase().split("\\s+").length;
                    return wordCount >= minLength && wordCount <= maxLength;
                })
                .collect(Collectors.toList());
    }
    
    public List<RAKEScore> extractKeywordsWithScoreThreshold(String text, double minScore) {
        List<RAKEScore> allScores = extractKeywords(text);
        
        return allScores.stream()
                .filter(score -> score.getScore() >= minScore)
                .collect(Collectors.toList());
    }
    
    public Map<String, List<String>> extractKeywordsByCategory(String text) {
        List<RAKEScore> scores = extractKeywords(text);
        Map<String, List<String>> categorizedKeywords = new HashMap<>();
        
        for (RAKEScore score : scores) {
            String phrase = score.getPhrase();
            int wordCount = phrase.split("\\s+").length;
            
            String category;
            if (wordCount == 1) {
                category = "single";
            } else if (wordCount == 2) {
                category = "double";
            } else if (wordCount == 3) {
                category = "triple";
            } else {
                category = "long";
            }
            
            categorizedKeywords.computeIfAbsent(category, k -> new ArrayList<>()).add(phrase);
        }
        
        return categorizedKeywords;
    }
    
    public List<RAKEScore> extractKeywordsFromMultipleDocuments(List<String> documents) {
        Map<String, Double> aggregatedScores = new HashMap<>();
        Map<String, Integer> documentFrequency = new HashMap<>();
        
        for (String document : documents) {
            List<RAKEScore> docScores = extractKeywords(document);
            
            for (RAKEScore score : docScores) {
                String phrase = score.getPhrase();
                aggregatedScores.merge(phrase, score.getScore(), Double::sum);
                documentFrequency.merge(phrase, 1, Integer::sum);
            }
        }
        
        final int totalDocuments = documents.size();
        return aggregatedScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> new RAKEScore(
                        entry.getKey(),
                        entry.getValue() / documentFrequency.get(entry.getKey())
                ))
                .collect(Collectors.toList());
    }
    
    public static class RAKEScore {
        private final String phrase;
        private final double score;
        
        public RAKEScore(String phrase, double score) {
            this.phrase = phrase;
            this.score = score;
        }
        
        public String getPhrase() {
            return phrase;
        }
        
        public double getScore() {
            return score;
        }
        
        public int getWordCount() {
            return phrase.split("\\s+").length;
        }
        
        @Override
        public String toString() {
            return String.format("%s: %.4f", phrase, score);
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RAKEScore rakeScore = (RAKEScore) o;
            return Double.compare(rakeScore.score, score) == 0 && Objects.equals(phrase, rakeScore.phrase);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(phrase, score);
        }
    }
}
