package com.seo.keywordgenerator.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SEOOptimizer {

    private final TextPreprocessor textPreprocessor;
    
    private static final List<String> SEO_MODIFIERS = Arrays.asList(
            "best", "guide", "tutorial", "top", "ultimate", "complete", "advanced",
            "professional", "expert", "step by step", "for beginners", "easy",
            "quick", "comprehensive", "detailed", "practical", "modern", "latest"
    );
    
    private static final List<String> SPAMMY_WORDS = Arrays.asList(
            "free", "cheap", "discount", "sale", "buy now", "click here", "limited time",
            "act now", "don't miss", "exclusive", "instant", "guaranteed", "miracle",
            "breakthrough", "revolutionary", "amazing", "incredible", "unbelievable",
            "shocking", "secret", "hidden", "revealed", "exposed", "banned", "illegal",
            "hack", "cheat", "trick", "manipulation", "scam", "fraud", "fake", "bot",
            "auto", "automated", "mass", "bulk", "unlimited", "endless", "forever",
            "overnight", "instantly", "immediately", "quickly", "fast", "rapid"
    );
    
    private static final List<String> LOW_QUALITY_PATTERNS = Arrays.asList(
            "\\d+", "\\$", "%", "!", "\\+", "\\*", "#", "@", "&", "http", "www", ".com"
    );
    
    private static final Pattern SPAM_PATTERN = Pattern.compile(
            String.join("|", SPAMMY_WORDS.stream().map(Pattern::quote).collect(Collectors.toList())),
            Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern LOW_QUALITY_PATTERN = Pattern.compile(
            String.join("|", LOW_QUALITY_PATTERNS)
    );

    public SEOOptimizer(TextPreprocessor textPreprocessor) {
        this.textPreprocessor = textPreprocessor;
    }
    
    public List<OptimizedKeyword> optimizeKeywords(List<String> keywords) {
        return optimizeKeywords(keywords, 50);
    }
    
    public List<OptimizedKeyword> optimizeKeywords(List<String> keywords, int maxKeywords) {
        if (keywords == null || keywords.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> cleanKeywords = preprocessKeywords(keywords);
        List<String> filteredKeywords = filterSpammyKeywords(cleanKeywords);
        
        List<OptimizedKeyword> optimizedKeywords = new ArrayList<>();
        
        optimizedKeywords.addAll(createOptimizedVersions(filteredKeywords));
        
        return optimizedKeywords.stream()
                .sorted((a, b) -> Double.compare(b.getQualityScore(), a.getQualityScore()))
                .limit(maxKeywords)
                .collect(Collectors.toList());
    }
    
    public List<OptimizedKeyword> optimizeWithModifiers(List<String> keywords) {
        return optimizeWithModifiers(keywords, Arrays.asList("best", "guide", "tutorial", "top"));
    }
    
    public List<OptimizedKeyword> optimizeWithModifiers(List<String> keywords, List<String> customModifiers) {
        if (keywords == null || keywords.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> cleanKeywords = preprocessKeywords(keywords);
        List<String> filteredKeywords = filterSpammyKeywords(cleanKeywords);
        
        List<OptimizedKeyword> optimizedKeywords = new ArrayList<>();
        
        for (String keyword : filteredKeywords) {
            for (String modifier : customModifiers) {
                String modifiedKeyword = addModifier(keyword, modifier);
                if (modifiedKeyword != null && !modifiedKeyword.equals(keyword)) {
                    double qualityScore = calculateQualityScore(modifiedKeyword);
                    optimizedKeywords.add(new OptimizedKeyword(
                            modifiedKeyword,
                            qualityScore,
                            getKeywordType(modifiedKeyword),
                            "MODIFIED",
                            modifier
                    ));
                }
            }
        }
        
        return optimizedKeywords.stream()
                .sorted((a, b) -> Double.compare(b.getQualityScore(), a.getQualityScore()))
                .collect(Collectors.toList());
    }
    
    public List<OptimizedKeyword> preferLongTailKeywords(List<String> keywords) {
        return preferLongTailKeywords(keywords, 30);
    }
    
    public List<OptimizedKeyword> preferLongTailKeywords(List<String> keywords, int maxKeywords) {
        if (keywords == null || keywords.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> cleanKeywords = preprocessKeywords(keywords);
        List<String> filteredKeywords = filterSpammyKeywords(cleanKeywords);
        
        List<OptimizedKeyword> optimizedKeywords = new ArrayList<>();
        
        for (String keyword : filteredKeywords) {
            double qualityScore = calculateQualityScore(keyword);
            String type = getKeywordType(keyword);
            double longTailBonus = calculateLongTailBonus(keyword);
            
            optimizedKeywords.add(new OptimizedKeyword(
                    keyword,
                    qualityScore + longTailBonus,
                    type,
                    "ORIGINAL",
                    null
            ));
        }
        
        return optimizedKeywords.stream()
                .sorted((a, b) -> Double.compare(b.getQualityScore(), a.getQualityScore()))
                .limit(maxKeywords)
                .collect(Collectors.toList());
    }
    
    private List<String> preprocessKeywords(List<String> keywords) {
        return keywords.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .map(textPreprocessor::preprocessText)
                .filter(keyword -> keyword.length() >= 2)
                .distinct()
                .collect(Collectors.toList());
    }
    
    private List<String> filterSpammyKeywords(List<String> keywords) {
        return keywords.stream()
                .filter(keyword -> !isSpammy(keyword))
                .filter(keyword -> !isLowQuality(keyword))
                .filter(keyword -> keyword.split("\\s+").length <= 6)
                .collect(Collectors.toList());
    }
    
    private boolean isSpammy(String keyword) {
        return SPAM_PATTERN.matcher(keyword).find();
    }
    
    private boolean isLowQuality(String keyword) {
        return LOW_QUALITY_PATTERN.matcher(keyword).find();
    }
    
    private List<OptimizedKeyword> createOptimizedVersions(List<String> keywords) {
        List<OptimizedKeyword> optimizedKeywords = new ArrayList<>();
        
        for (String keyword : keywords) {
            double qualityScore = calculateQualityScore(keyword);
            String type = getKeywordType(keyword);
            
            optimizedKeywords.add(new OptimizedKeyword(
                    keyword,
                    qualityScore,
                    type,
                    "ORIGINAL",
                    null
            ));
            
            for (String modifier : SEO_MODIFIERS.subList(0, Math.min(8, SEO_MODIFIERS.size()))) {
                String modifiedKeyword = addModifier(keyword, modifier);
                if (modifiedKeyword != null && !modifiedKeyword.equals(keyword)) {
                    double modifiedScore = calculateQualityScore(modifiedKeyword);
                    optimizedKeywords.add(new OptimizedKeyword(
                            modifiedKeyword,
                            modifiedScore,
                            getKeywordType(modifiedKeyword),
                            "MODIFIED",
                            modifier
                    ));
                }
            }
        }
        
        return optimizedKeywords;
    }
    
    private String addModifier(String keyword, String modifier) {
        if (keyword.toLowerCase().contains(modifier.toLowerCase())) {
            return keyword;
        }
        
        String[] words = keyword.split("\\s+");
        
        if (modifier.matches("\\b(step by step|for beginners)\\b")) {
            return modifier + " " + keyword;
        }
        
        if (words.length == 1) {
            return modifier + " " + keyword;
        }
        
        if (words.length == 2) {
            return modifier + " " + keyword;
        }
        
        if (words.length >= 3) {
            return keyword + " " + modifier;
        }
        
        return keyword;
    }
    
    private double calculateQualityScore(String keyword) {
        double baseScore = 5.0;
        
        int wordCount = keyword.split("\\s+").length;
        double lengthScore = calculateLengthScore(wordCount);
        
        double modifierScore = calculateModifierScore(keyword);
        
        double readabilityScore = calculateReadabilityScore(keyword);
        
        double spamPenalty = calculateSpamPenalty(keyword);
        
        double finalScore = baseScore + lengthScore + modifierScore + readabilityScore - spamPenalty;
        
        // Add randomness to prevent identical scores
        finalScore += (Math.random() - 0.5) * 0.5; // ±0.25 random variation
        
        return Math.max(0.0, Math.min(10.0, finalScore));
    }
    
    private double calculateLengthScore(int wordCount) {
        // Balanced scoring to prevent domination of any type
        if (wordCount == 1) return 1.0;      // Short keywords get good score
        if (wordCount == 2) return 1.5;      // Medium get best score
        if (wordCount == 3) return 1.2;      // Long-tail get moderate score
        if (wordCount == 4) return 0.8;      // Very long get lower
        if (wordCount == 5) return 0.5;      
        return 0.2;  // Very long tail get lowest
    }
    
    private double calculateModifierScore(String keyword) {
        double score = 0.0;
        String lowerKeyword = keyword.toLowerCase();
        
        for (String modifier : SEO_MODIFIERS) {
            if (lowerKeyword.contains(modifier)) {
                if (modifier.equals("best") || modifier.equals("top")) {
                    score += 2.0;
                } else if (modifier.equals("guide") || modifier.equals("tutorial")) {
                    score += 1.8;
                } else if (modifier.equals("ultimate") || modifier.equals("complete")) {
                    score += 1.5;
                } else {
                    score += 1.0;
                }
            }
        }
        
        return score;
    }
    
    private double calculateReadabilityScore(String keyword) {
        String[] words = keyword.split("\\s+");
        double avgWordLength = Arrays.stream(words)
                .mapToInt(String::length)
                .average()
                .orElse(0.0);
        
        if (avgWordLength >= 4 && avgWordLength <= 7) {
            return 1.0;
        } else if (avgWordLength >= 3 && avgWordLength <= 8) {
            return 0.5;
        } else {
            return 0.0;
        }
    }
    
    private double calculateSpamPenalty(String keyword) {
        double penalty = 0.0;
        String lowerKeyword = keyword.toLowerCase();
        
        for (String spamWord : SPAMMY_WORDS) {
            if (lowerKeyword.contains(spamWord)) {
                penalty += 3.0;
            }
        }
        
        if (LOW_QUALITY_PATTERN.matcher(keyword).find()) {
            penalty += 2.0;
        }
        
        if (keyword.split("\\s+").length > 6) {
            penalty += 1.0;
        }
        
        return penalty;
    }
    
    private double calculateLongTailBonus(String keyword) {
        int wordCount = keyword.split("\\s+").length;
        
        // Reduced long-tail bonus to prevent domination
        if (wordCount >= 3) {
            return (wordCount - 2) * 0.1; // Changed from 0.5 to 0.1
        }
        
        return 0.0;
    }
    
    private String getKeywordType(String keyword) {
        int wordCount = keyword.split("\\s+").length;
        
        if (wordCount == 1) {
            return "SHORT";
        } else if (wordCount == 2) {
            return "MEDIUM";
        } else {
            return "LONG_TAIL";
        }
    }
    
    public Map<String, List<OptimizedKeyword>> categorizeOptimizedKeywords(List<String> keywords) {
        List<OptimizedKeyword> optimized = optimizeKeywords(keywords);
        
        return optimized.stream()
                .collect(Collectors.groupingBy(
                        OptimizedKeyword::getType,
                        Collectors.toList()
                ));
    }
    
    public List<OptimizedKeyword> getTopQualityKeywords(List<String> keywords, double minQuality) {
        List<OptimizedKeyword> optimized = optimizeKeywords(keywords);
        
        return optimized.stream()
                .filter(k -> k.getQualityScore() >= minQuality)
                .sorted((a, b) -> Double.compare(b.getQualityScore(), a.getQualityScore()))
                .collect(Collectors.toList());
    }
    
    public static class OptimizedKeyword {
        private final String keyword;
        private final double qualityScore;
        private final String type;
        private final String optimizationType;
        private final String modifier;
        
        public OptimizedKeyword(String keyword, double qualityScore, String type, 
                               String optimizationType, String modifier) {
            this.keyword = keyword;
            this.qualityScore = qualityScore;
            this.type = type;
            this.optimizationType = optimizationType;
            this.modifier = modifier;
        }
        
        public String getKeyword() {
            return keyword;
        }
        
        public double getQualityScore() {
            return qualityScore;
        }
        
        public String getType() {
            return type;
        }
        
        public String getOptimizationType() {
            return optimizationType;
        }
        
        public String getModifier() {
            return modifier;
        }
        
        public int getWordCount() {
            return keyword.split("\\s+").length;
        }
        
        public boolean isLongTail() {
            return getWordCount() >= 3;
        }
        
        public boolean isModified() {
            return "MODIFIED".equals(optimizationType);
        }
        
        @Override
        public String toString() {
            return String.format("%s [%s] Score: %.2f (%s)", 
                    keyword, type, qualityScore, optimizationType);
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OptimizedKeyword that = (OptimizedKeyword) o;
            return Objects.equals(keyword, that.keyword);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(keyword);
        }
    }
}
