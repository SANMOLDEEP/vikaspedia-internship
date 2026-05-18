package com.seo.keywordgenerator.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class KeywordGeneratorEngineNew {
    
    /**
     * Intent categories for professional SEO-level keyword generation
     */
    public enum IntentType {
        LEARNING,
        PROJECT,
        INTERVIEW,
        COMPARISON,
        PROBLEM_SOLVING,
        CAREER,
        USE_CASE
    }

    private final TextPreprocessor textPreprocessor;
    private final RAKEExtractor rakeExtractor;
    
    // Template patterns for natural keyword generation
    private static final String[] NATURAL_TEMPLATES = {
        "learn {topic} for beginners",
        "best {topic} tutorial", 
        "{topic} examples with explanation",
        "how to learn {topic}",
        "{topic} step by step guide",
        "complete {topic} course",
        "{topic} for beginners guide",
        "top {topic} interview questions",
        "{topic} project ideas for beginners",
        "{topic} tutorial for beginners",
        "easy {topic} projects",
        "advanced {topic} techniques",
        "{topic} best practices",
        "professional {topic} development",
        "{topic} complete guide",
        "free {topic} tutorial",
        "{topic} online course",
        "{topic} certification guide"
    };
    
    // Question templates
    private static final String[] QUESTION_TEMPLATES = {
        "how to learn {topic}",
        "what is {topic}",
        "why use {topic}",
        "when to use {topic}",
        "where to learn {topic}",
        "{topic} vs alternatives",
        "best {topic} for beginners",
        "is {topic} easy to learn",
        "how long to learn {topic}",
        "can i learn {topic} without",
        "should i learn {topic}"
    };
    
    // Common programming/technical terms for topic identification
    private static final List<String> TECH_TERMS = Arrays.asList(
        "react", "javascript", "python", "java", "node", "angular", "vue", 
        "docker", "api", "database", "frontend", "backend", "spring",
        "html", "css", "typescript", "mongodb", "mysql", "redis", "git",
        "mobile", "app", "android", "ios", "ui", "ux", "design", "development",
        "swift", "kotlin", "flutter", "react-native", "xamarin", "cordova",
        "user", "experience", "interface", "psychology", "visual", "aesthetics",
        "functionality", "digital", "products", "beautiful", "effortless",
        "research", "personas", "empathy", "mapping", "stakeholder", "interviews",
        "needs", "phase", "methods", "human-centred", "discipline", "blends"
    );
    
    // Intent keywords for context extraction
    private static final Map<String, List<String>> INTENT_KEYWORDS = Map.of(
        "skill_level", Arrays.asList("beginners", "beginner", "advanced", "intermediate", "expert", "basic", "professional"),
        "content_type", Arrays.asList("examples", "tutorial", "guide", "course", "book", "documentation", "reference", "manual"),
        "action", Arrays.asList("learn", "build", "create", "develop", "implement", "design", "optimize", "master", "understand"),
        "format", Arrays.asList("step", "by", "with", "for", "in", "to", "from", "on", "of", "and", "or", "but"),
        "quality", Arrays.asList("best", "top", "ultimate", "complete", "comprehensive", "easy", "quick", "simple", "practical")
    );
    
    public KeywordGeneratorEngineNew(TextPreprocessor textPreprocessor, RAKEExtractor rakeExtractor) {
        this.textPreprocessor = textPreprocessor;
        this.rakeExtractor = rakeExtractor;
    }
    
    public List<GeneratedKeyword> generateKeywords(List<String> extractedWords) {
        return generateKeywords(extractedWords, 50, "");
    }
    
    public List<GeneratedKeyword> generateKeywords(List<String> extractedWords, int maxKeywords) {
        return generateKeywords(extractedWords, maxKeywords, "");
    }
    
    public List<GeneratedKeyword> generateKeywords(List<String> extractedWords, int maxKeywords, String originalContent) {
        if (extractedWords == null || extractedWords.isEmpty()) {
            return Collections.emptyList();
        }
        
        Set<String> uniqueKeywords = new LinkedHashSet<>();
        List<GeneratedKeyword> generatedKeywords = new ArrayList<>();
        
        List<String> cleanWords = preprocessExtractedWords(extractedWords);
        
        // Generate natural, template-based keywords
        List<GeneratedKeyword> naturalKeywords = generateNaturalKeywords(cleanWords, uniqueKeywords, originalContent);
        generatedKeywords.addAll(naturalKeywords);

        return generatedKeywords.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(maxKeywords)
                .collect(Collectors.toList());
    }
    
    /**
     * Preprocess extracted words
     */
    private List<String> preprocessExtractedWords(List<String> extractedWords) {
        return extractedWords.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(word -> !word.isEmpty())
                .map(textPreprocessor::preprocessText)
                .filter(word -> word.length() >= 2)
                .distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * Generate intent-aware, context-sensitive keywords
     */
    private List<GeneratedKeyword> generateNaturalKeywords(List<String> cleanWords, Set<String> uniqueKeywords, String originalContent) {
        List<GeneratedKeyword> keywords = new ArrayList<>();
        
        // Extract multiple topics and intent
        List<String> mainTopics = extractMainTopics(cleanWords, originalContent);
        if (mainTopics.isEmpty()) {
            log.warn("No main topics found in extracted words");
            return keywords;
        }
        
        // Extract user intent from input
        UserIntent intent = extractUserIntent(cleanWords);
        log.info("Extracted intent: skill_level={}, content_type={}, action={}", 
                intent.skillLevel, intent.contentType, intent.action);
        
        // Generate short-tail keywords (Layer 1) - use all 3 topics
        keywords.addAll(generateShortTailKeywords(mainTopics, uniqueKeywords));
        
        // Generate intent-aware keywords (Layer 2) - rotate through topics
        keywords.addAll(generateIntentAwareKeywords(mainTopics, intent, uniqueKeywords));
        
        // Generate question-based keywords (lower priority) - rotate through topics
        keywords.addAll(generateQuestionKeywords(mainTopics, intent, uniqueKeywords));
        
        log.info("Generated {} intent-aware keywords for topics: {}", keywords.size(), mainTopics);
        return keywords;
    }
    
    /**
     * Generate short-tail keywords (Layer 1)
     */
    private List<GeneratedKeyword> generateShortTailKeywords(List<String> mainTopics, Set<String> uniqueKeywords) {
        List<GeneratedKeyword> keywords = new ArrayList<>();

        // Generate keywords for each of the top 3 RAKE-extracted topics/phrases
        for (int i = 0; i < mainTopics.size(); i++) {
            String topic = mainTopics.get(i);
            
            List<String> shortKeywords = Arrays.asList(
                topic,
                topic + " tutorial",
                topic + " examples",
                topic + " course",
                topic + " projects",
                topic + " guide",
                topic + " basics",
                topic + " interview questions"
            );

            for (String keyword : shortKeywords) {
                if (!uniqueKeywords.contains(keyword)) {
                    double score = 70 + (keyword.split("\\s+").length == 1 ? 10 : 5); // strong base
                    keywords.add(new GeneratedKeyword(keyword, "SHORT_TAIL", score));
                    uniqueKeywords.add(keyword);
                }
            }
        }

        return keywords;
    }
    
    /**
     * Extract user intent from input words
     */
    private UserIntent extractUserIntent(List<String> cleanWords) {
        UserIntent intent = new UserIntent();
        
        for (String word : cleanWords) {
            String lowerWord = word.toLowerCase();
            
            // Check skill level
            if (INTENT_KEYWORDS.get("skill_level").contains(lowerWord)) {
                intent.skillLevel = lowerWord;
            }
            
            // Check content type
            if (INTENT_KEYWORDS.get("content_type").contains(lowerWord)) {
                intent.contentType = lowerWord;
            }
            
            // Check action
            if (INTENT_KEYWORDS.get("action").contains(lowerWord)) {
                intent.action = lowerWord;
            }
            
            // Check quality indicators
            if (INTENT_KEYWORDS.get("quality").contains(lowerWord)) {
                intent.quality = lowerWord;
            }
        }
        
        return intent;
    }
    
    /**
     * Generate intent-aware keywords based on extracted intent
     */
    private List<GeneratedKeyword> generateIntentAwareKeywords(List<String> mainTopics, UserIntent intent, Set<String> uniqueKeywords) {
        List<GeneratedKeyword> keywords = new ArrayList<>();
        
        // Extract context words for better diversity
        Set<String> contextWords = extractContextWords(uniqueKeywords);
        
        // Generate high-relevance templates based on intent
        List<String> intentTemplates = getIntentSpecificTemplates(intent);
        
        // Pattern counting for diversity
        Map<String, Integer> patternCount = new HashMap<>();
        
        for (int i = 0; i < intentTemplates.size(); i++) {
            String template = intentTemplates.get(i);
            // Rotate through RAKE-extracted topics/phrases for diversity
            String topic = mainTopics.get(i % mainTopics.size());
            String keyword = template.replace("{topic}", topic);
            
            // Apply final normalization
            keyword = normalizeKeyword(keyword);
            
            if (isValidIntentKeyword(keyword, intent) && !uniqueKeywords.contains(keyword)) {
                // Check for similarity with existing keywords
                if (!isSimilarToExisting(keyword, keywords)) {
                    double score = calculateIntentAwareScore(keyword, topic, intent);
                    keywords.add(new GeneratedKeyword(keyword, "INTENT_AWARE", score));
                    uniqueKeywords.add(keyword);
                    patternCount.put(getPatternType(template), patternCount.getOrDefault(getPatternType(template), 0) + 1);
                }
            }
        }
        
        return keywords;
    }
    
    /**
     * Extract context words for better diversity
     */
    private Set<String> extractContextWords(Set<String> uniqueKeywords) {
        Set<String> contextWords = new HashSet<>();
        
        // Common React/JavaScript context terms
        String[] reactContext = {"component", "hooks", "state", "props", "jsx", "router", "redux", "api", "async", "fetch"};
        String[] generalContext = {"tutorial", "guide", "example", "project", "exercise", "practice", "hands-on", "interactive"};
        
        contextWords.addAll(Arrays.asList(reactContext));
        contextWords.addAll(Arrays.asList(generalContext));
        
        return contextWords;
    }
    
    /**
     * Get pattern type for diversity control
     */
    private String getPatternType(String template) {
        if (template.contains("for beginners")) return "beginners";
        if (template.contains("tutorial")) return "tutorial";
        if (template.contains("examples")) return "examples";
        if (template.contains("guide")) return "guide";
        if (template.contains("project")) return "project";
        if (template.contains("learn")) return "learn";
        return "other";
    }
    
    /**
     * Expand keyword with context for better diversity (deterministic)
     */
    private String expandWithContext(String keyword, Set<String> contextWords) {
        // Deterministic selection based on keyword hash
        int hash = Math.abs(keyword.hashCode());
        
        // Add context words to 15% of keywords (deterministic)
        if (hash % 100 < 15 && !contextWords.isEmpty()) {
            String contextWord = getContextWord(contextWords, hash);
            
            if (!contextWord.isEmpty() && !keyword.toLowerCase().contains(contextWord.toLowerCase())) {
                // Add context word in appropriate position
                if (keyword.contains("examples")) {
                    keyword = keyword.replace("examples", contextWord + " examples");
                } else if (keyword.contains("tutorial")) {
                    keyword = keyword.replace("tutorial", contextWord + " tutorial");
                } else if (keyword.contains("projects")) {
                    keyword = keyword.replace("projects", contextWord + " projects");
                }
            }
        }
        
        return keyword;
    }
    
    /**
     * Get context word deterministically
     */
    private String getContextWord(Set<String> contextWords, int index) {
        List<String> list = new ArrayList<>(contextWords);
        return list.get(index % list.size());
    }
    
    /**
     * Get intent-specific templates
     */
    private List<String> getIntentSpecificTemplates(UserIntent intent) {
        List<String> templates = new ArrayList<>();
        
        // ALWAYS generate the high-value combinations first
        if (intent.skillLevel != null && intent.contentType != null) {
            // Must-have combinations
            templates.add("{topic} " + intent.contentType + " for " + intent.skillLevel);
            templates.add("learn {topic} for " + intent.skillLevel + " with " + intent.contentType);
            templates.add(intent.skillLevel + " guide to {topic} with " + intent.contentType);
            templates.add("{topic} " + intent.contentType + " tutorial for " + intent.skillLevel);
        }
        
        // Individual intent-based templates
        if (intent.skillLevel != null) {
            templates.add("learn {topic} for " + intent.skillLevel);
            templates.add("{topic} for " + intent.skillLevel + " guide");
            templates.add("best {topic} course for " + intent.skillLevel);
            templates.add("easy {topic} for " + intent.skillLevel);
        }
        
        if (intent.contentType != null) {
            templates.add("{topic} " + intent.contentType + " for beginners");
            templates.add("learn {topic} with " + intent.contentType);
            templates.add("{topic} " + intent.contentType + " tutorial");
            templates.add("practical {topic} " + intent.contentType);
            
            // Practical intent boost for examples
            if ("examples".equals(intent.contentType)) {
                templates.add("{topic} real world examples");
                templates.add("{topic} practical examples");
                templates.add("{topic} hands on tutorial");
                templates.add("{topic} mini projects for beginners");
                templates.add("{topic} coding exercises");
                templates.add("{topic} practice problems");
                templates.add("{topic} interactive examples");
            }
        }
        
        if (intent.action != null) {
            templates.add(intent.action + " {topic} step by step");
            templates.add("how to " + intent.action + " {topic}");
            templates.add(intent.action + " {topic} from scratch");
        }
        
        // High-value SEO templates by intent category
        templates.addAll(Arrays.asList(
            // Learning intent
            "learn {topic} for beginners",
            "best {topic} tutorial",
            "how to learn {topic}",
            "complete {topic} course",
            "{topic} step by step guide",
            
            // Project intent
            "{topic} mini projects",
            "{topic} project ideas",
            "practical {topic} projects",
            "{topic} projects for beginners with examples",
            "{topic} hands on projects",
            
            // Problem-solving intent
            "{topic} common mistakes",
            "{topic} debugging guide",
            "{topic} performance optimization",
            "{topic} troubleshooting",
            "{topic} best practices",
            
            // Career intent
            "{topic} roadmap 2026",
            "{topic} developer skills",
            "{topic} interview questions",
            "{topic} job preparation",
            
            // Use-case intent
            "{topic} real world applications",
            "{topic} use cases in industry",
            "{topic} practical examples",
            "{topic} business applications",
            
            // General high-value
            "{topic} examples",
            "{topic} guide"
        ));
        
        return templates;
    }
    
    /**
     * Replace intent placeholders in templates
     */
    private String replaceIntentPlaceholders(String template, UserIntent intent) {
        String result = template;
        
        if (intent.skillLevel != null) {
            result = result.replace("{skill_level}", intent.skillLevel);
        }
        if (intent.contentType != null) {
            result = result.replace("{content_type}", intent.contentType);
        }
        if (intent.action != null) {
            result = result.replace("{action}", intent.action);
        }
        
        return result;
    }
    
    /**
     * Validate if keyword matches user intent
     */
    private boolean isValidIntentKeyword(String keyword, UserIntent intent) {
        // Basic validation
        if (keyword == null || keyword.trim().isEmpty()) return false;
        
        // CRITICAL: Check keyword length to prevent extremely long keywords
        if (keyword.length() > 80) return false; // Hard limit for keyword length
        
        // Check for duplicate words
        String[] words = keyword.toLowerCase().split("\\s+");
        Set<String> uniqueWords = Arrays.stream(words).collect(Collectors.toSet());
        if (uniqueWords.size() != words.length) return false;
        
        // Check word count
        if (words.length < 2 || words.length > 6) return false;
        
        // Filter out irrelevant keywords (certification, vs alternatives, etc.)
        String lowerKeyword = keyword.toLowerCase();
        if (lowerKeyword.contains("certification") || lowerKeyword.contains("vs alternatives")) {
            return false; // Filter out irrelevant keywords
        }
        
        return true;
    }
    
    /**
     * Calculate intent-aware score
     */
    private double calculateIntentAwareScore(String keyword, String mainTopic, UserIntent intent) {
        double score = 50.0; // Base score
        
        // High bonus for matching intent
        if (intent.skillLevel != null && keyword.toLowerCase().contains(intent.skillLevel)) {
            score += 25;
        }
        if (intent.contentType != null && keyword.toLowerCase().contains(intent.contentType)) {
            score += 25;
        }
        if (intent.action != null && keyword.toLowerCase().contains(intent.action)) {
            score += 20;
        }
        
        // Extra bonus for strong intent combinations (examples + beginners)
        if (intent.skillLevel != null && intent.contentType != null && 
            keyword.toLowerCase().contains(intent.skillLevel) && 
            keyword.toLowerCase().contains(intent.contentType)) {
            score += 15; // Bonus for having both intent elements
        }
        
        // Length bonus (prefer 3-4 word keywords)
        int wordCount = keyword.split("\\s+").length;
        if (wordCount == 3 || wordCount == 4) {
            score += 10;
        } else if (wordCount == 2) {
            score += 8; // boost instead of penalty
        } else if (wordCount > 4) {
            score -= 5; // Penalty for too long keywords
        }
        
        // Natural pattern bonus
        if (keyword.contains(" for ") || keyword.contains(" with ")) {
            score += 8;
        }
        
        // HIGH-INTENT keyword boosts
        if (keyword.contains("projects") || keyword.contains("examples")) {
            score += 10; // High engagement keywords
        }
        if (keyword.contains("interview")) {
            score += 8;
        }
        if (keyword.contains("mistakes") || keyword.contains("errors") || keyword.contains("debugging")) {
            score += 7;
        }
        if (keyword.contains("roadmap") || keyword.contains("skills") || keyword.contains("career")) {
            score += 6;
        }
        if (keyword.contains("real world") || keyword.contains("practical") || keyword.contains("hands on")) {
            score += 5;
        }
        
        // PENALTIES for generic keywords
        String lowerKeyword = keyword.toLowerCase();
        if (lowerKeyword.startsWith("what is ") || lowerKeyword.startsWith("why use ") || 
            lowerKeyword.startsWith("when to use ") || lowerKeyword.startsWith("where to ")) {
            score -= 15; // Heavy penalty for generic questions
        }
        
        if (lowerKeyword.contains("vs ") || lowerKeyword.contains("versus ") || 
            lowerKeyword.contains("comparison ")) {
            score -= 10; // Penalty for comparison keywords
        }
        
        // Stronger penalty for overused patterns
        if (lowerKeyword.contains("for beginners")) {
            score -= 5; // Reduce repetition dominance
        }
        
        return Math.max(score, 20); // Minimum score of 20, maximum of 100
    }
    
    /**
     * User intent data class
     */
    private static class UserIntent {
        String skillLevel;
        String contentType;
        String action;
        String quality;
        
        @Override
        public String toString() {
            return String.format("UserIntent{skillLevel='%s', contentType='%s', action='%s', quality='%s'}", 
                    skillLevel, contentType, action, quality);
        }
    }
    
    /**
     * Extract topics using RAKE algorithm
     */
    private List<String> extractMainTopics(List<String> cleanWords, String originalContent) {
        if (cleanWords.isEmpty()) return Collections.singletonList("development");
        
        try {
            // Use RAKE to extract keyword phrases from original content
            log.debug("RAKE processing content: {}", originalContent);
            log.debug("Clean words for fallback: {}", cleanWords);
            
            List<RAKEExtractor.RAKEScore> rakeScores = rakeExtractor.extractKeywords(originalContent);
            log.debug("RAKE extracted {} phrases: {}", rakeScores.size(), 
                rakeScores.stream().map(RAKEExtractor.RAKEScore::getPhrase).collect(Collectors.toList()));
            
            // Get top scoring phrases as topics
            List<String> rakeTopics = rakeScores.stream()
                    .sorted(Comparator.comparingDouble(RAKEExtractor.RAKEScore::getScore).reversed())
                    .limit(3)
                    .map(RAKEExtractor.RAKEScore::getPhrase)
                    .collect(Collectors.toList());
            
            if (!rakeTopics.isEmpty()) {
                log.info("RAKE successfully extracted topics: {}", rakeTopics);
                return rakeTopics;
            }
            
            log.warn("RAKE extraction returned empty, falling back to word-based extraction");
            return extractTopicsFallback(cleanWords);
        } catch (Exception e) {
            log.warn("RAKE extraction failed, falling back to word-based extraction: {}", e.getMessage());
            return extractTopicsFallback(cleanWords);
        }
    }
    
    /**
     * Fallback topic extraction method
     */
    private List<String> extractTopicsFallback(List<String> cleanWords) {
        log.debug("Starting fallback topic extraction with words: {}", cleanWords);
        
        // Priority to high-value technical terms with better scoring
        Map<String, Integer> topicScores = new HashMap<>();
        
        for (String word : cleanWords) {
            String lowerWord = word.toLowerCase();
            
            // Skip very long words and stop words
            if (lowerWord.length() > 25 || isStopWord(lowerWord)) continue;
            
            int score = 0;
            
            // High priority for core technical terms
            if (TECH_TERMS.contains(lowerWord)) {
                score += 50;
                log.debug("Found tech term: {} with score 50", lowerWord);
                // Extra bonus for high-value specific terms
                if (lowerWord.equals("design") || lowerWord.contains("design")) {
                    score += 50; // Maximum priority for design terms
                } else if (lowerWord.equals("ux") || lowerWord.contains("ux") || 
                    lowerWord.equals("user") || lowerWord.contains("user") ||
                    lowerWord.equals("experience") || lowerWord.contains("experience") ||
                    lowerWord.equals("interface") || lowerWord.contains("interface") ||
                    lowerWord.equals("visual") || lowerWord.contains("visual")) {
                    score += 40; // High priority for UX terms
                } else if (lowerWord.contains("mobile") || lowerWord.contains("app") || 
                    lowerWord.contains("ui") || lowerWord.contains("development") || 
                    lowerWord.contains("react") || lowerWord.contains("android") || 
                    lowerWord.contains("ios")) {
                    score += 30; // High priority for mobile terms
                } else if (lowerWord.equals("technical") || lowerWord.equals("functionality") || 
                    lowerWord.equals("digital") || lowerWord.equals("products")) {
                    score += 10; // Lower priority for generic terms
                }
            }
            
            // Score based on position in text (earlier words get higher score)
            int position = cleanWords.indexOf(word);
            if (position < cleanWords.size() / 3) score += 20; // First third of content
            else if (position < 2 * cleanWords.size() / 3) score += 10; // Middle third
            
            // Score based on word characteristics
            if (lowerWord.length() >= 4 && lowerWord.length() <= 12) score += 15;
            if (lowerWord.length() >= 6 && lowerWord.length() <= 10) score += 10;
            if (Character.isUpperCase(word.charAt(0))) score += 8; // Capitalized words are often topics
            
            // Bonus for compound words (like "mobile", "app", "development")
            if (lowerWord.length() >= 5 && lowerWord.length() <= 10) score += 5;
            
            topicScores.put(lowerWord, score);
        }
        
        // Sort topics by score and return top 3
        List<String> resultTopics = topicScores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        log.debug("Final extracted topics: {}", resultTopics);
        return resultTopics;
    }
    
        
    /**
     * Check if word is a common stop word
     */
    private boolean isStopWord(String word) {
        String[] stopWords = {
            "the", "is", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", 
            "of", "with", "by", "from", "as", "was", "were", "been", "be", "have", "has",
            "had", "do", "does", "did", "will", "would", "could", "should", "may", "might",
            "can", "must", "this", "that", "these", "those", "i", "you", "he", "she", "it",
            "we", "they", "what", "which", "who", "when", "where", "why", "how", "all",
            "each", "every", "both", "few", "more", "most", "other", "some", "such",
            "only", "own", "same", "so", "than", "too", "very", "just", "now"
        };
        return Arrays.asList(stopWords).contains(word);
    }
    
    /**
     * Enhance keyword with context words
     */
    private String enhanceWithContextWords(String keyword, String template, Set<String> contextWords) {
        if (template.contains("for beginners") && !contextWords.isEmpty()) {
            String contextWord = contextWords.iterator().next();
            return keyword.replace("for beginners", "for " + contextWord);
        }
        
        if (template.contains("examples with") && !contextWords.isEmpty()) {
            String contextWord = contextWords.iterator().next();
            return keyword.replace("examples with", "examples with " + contextWord);
        }
        return keyword;
    }
    
    /**
     * Generate intent-aware question-based keywords
     */
    private List<GeneratedKeyword> generateQuestionKeywords(List<String> mainTopics, UserIntent intent, Set<String> uniqueKeywords) {
        List<GeneratedKeyword> keywords = new ArrayList<>();
        
        // Intent-aware question templates
        List<String> questionTemplates = new ArrayList<>();
        
        if (intent.skillLevel != null) {
            questionTemplates.add("how to learn {topic} for " + intent.skillLevel);
            questionTemplates.add("is {topic} easy for " + intent.skillLevel + " to learn");
            questionTemplates.add("what is the best way to learn {topic} for " + intent.skillLevel);
        }
        
        if (intent.contentType != null) {
            questionTemplates.add("where can I find {topic} " + intent.contentType);
            questionTemplates.add("what are the best {topic} " + intent.contentType);
        }
        
        // Add only high-value questions (lower priority)
        questionTemplates.addAll(Arrays.asList(
            "how to learn {topic}",
            "how long to learn {topic}",
            "where to learn {topic}"
        ));
        
        for (int i = 0; i < questionTemplates.size(); i++) {
            String template = questionTemplates.get(i);
            // Rotate through RAKE-extracted topics/phrases for diversity
            String topic = mainTopics.get(i % mainTopics.size());
            String keyword = template.replace("{topic}", topic);
            
            // Apply final normalization
            keyword = normalizeKeyword(keyword);
            
            if (isValidIntentKeyword(keyword, intent) && !uniqueKeywords.contains(keyword)) {
                double score = calculateQuestionKeywordScore(keyword);
                keywords.add(new GeneratedKeyword(keyword, "QUESTION", score));
                uniqueKeywords.add(keyword);
            }
        }
        
        return keywords;
    }
    
    /**
     * Final keyword normalization
     */
    private String normalizeKeyword(String keyword) {
        // Convert "easy react for beginners" → "easy react tutorial for beginners"
        if (keyword.startsWith("easy ") && keyword.contains(" for beginners") && 
            !keyword.contains("tutorial") && !keyword.contains("guide")) {
            keyword = keyword.replace("easy ", "easy ").replace(" for beginners", " tutorial for beginners");
        }
        
        // Convert "react guide" → "react tutorial guide" (for 2-word keywords)
        String[] words = keyword.split("\\s+");
        if (words.length == 2 && !keyword.contains("tutorial") && !keyword.contains("guide")) {
            keyword = keyword + " tutorial";
        }
        
        return keyword;
    }
    
    /**
     * Check if keyword is similar to existing keywords (deduplication)
     */
    private boolean isSimilarToExisting(String newKeyword, List<GeneratedKeyword> existingKeywords) {
        Set<String> newWords = new HashSet<>(Arrays.asList(newKeyword.toLowerCase().split("\\s+")));
        
        for (GeneratedKeyword existing : existingKeywords) {
            Set<String> existingWords = new HashSet<>(Arrays.asList(existing.getKeyword().toLowerCase().split("\\s+")));
            
            Set<String> intersection = new HashSet<>(newWords);
            intersection.retainAll(existingWords);
            
            double similarity = (double) intersection.size() / Math.max(newWords.size(), existingWords.size());
            if (similarity > 0.85) { // 85% similarity threshold (relaxed for short keywords)
                return true;
            }
        }
        return false;
    }
    
    /**
     * Validate if keyword is natural and search-friendly
     */
    private boolean isValidNaturalKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return false;
        
        // CRITICAL: Check keyword length to prevent extremely long keywords
        if (keyword.length() > 80) return false; // Hard limit for keyword length
        
        // Check for duplicate words
        String[] words = keyword.toLowerCase().split("\\s+");
        Set<String> uniqueWords = Arrays.stream(words).collect(Collectors.toSet());
        if (uniqueWords.size() != words.length) {
            return false; // Has duplicates
        }
        
        // Check word count (prefer 1-6 words - allow short-tail)
        int wordCount = words.length;
        if (wordCount < 1 || wordCount > 6) {
            return false; // Too short or too long
        }
        
        // Check for irrelevant patterns
        String lowerKeyword = keyword.toLowerCase();
        if (lowerKeyword.contains(" vs ") || lowerKeyword.contains(" versus ")) {
            return false; // Avoid "X react Y"
        }
        if (lowerKeyword.contains(" examples ") && lowerKeyword.contains(" examples")) {
            return false; // Avoid "X examples Y"
        }
        
        return true;
    }
    
    /**
     * Calculate score for natural keywords
     */
    private double calculateNaturalKeywordScore(String keyword, String mainTopic, String template) {
        double score = 50.0; // Base score
        
        // Length bonus (prefer 3-4 word keywords)
        int wordCount = keyword.split("\\s+").length;
        if (wordCount == 3 || wordCount == 4) {
            score += 15;
        } else if (wordCount == 2) {
            score += 10;
        } else if (wordCount > 4) {
            score -= 5;
        }
        
        // Topic relevance bonus
        if (keyword.toLowerCase().contains(mainTopic.toLowerCase())) {
            score += 20;
        }
        
        // Template-specific bonuses
        if (template.contains("for beginners") || template.contains("tutorial")) {
            score += 12;
        }
        if (template.contains("examples with") || template.contains("step by step")) {
            score += 10;
        }
        if (template.contains("best") || template.contains("top")) {
            score += 8;
        }
        if (template.startsWith("how ") || template.startsWith("what ")) {
            score += 15;
        }
        
        return Math.min(score, 100); // Cap at 100
    }
    
    /**
     * Calculate score for question keywords
     */
    private double calculateQuestionKeywordScore(String keyword) {
        double score = 45.0; // Base score for questions
        
        // Question words bonus
        if (keyword.startsWith("how ")) {
            score += 15;
        }
        if (keyword.startsWith("what ")) {
            score += 12;
        }
        if (keyword.startsWith("why ")) {
            score += 10;
        }
        
        // Length bonus
        int wordCount = keyword.split("\\s+").length;
        if (wordCount >= 3 && wordCount <= 5) {
            score += 8;
        }
        
        return Math.min(score, 100);
    }
    
    public static class GeneratedKeyword {
        private final String keyword;
        private final String type;
        private final double score;
        
        public GeneratedKeyword(String keyword, String type, double score) {
            this.keyword = keyword;
            this.type = type;
            this.score = score;
        }
        
        public String getKeyword() { return keyword; }
        public String getType() { return type; }
        public double getScore() { return score; }
    }
}
