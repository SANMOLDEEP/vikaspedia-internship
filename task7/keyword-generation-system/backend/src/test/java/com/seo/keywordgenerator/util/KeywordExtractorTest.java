package com.seo.keywordgenerator.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordExtractorTest {

    private final KeywordExtractor keywordExtractor = new KeywordExtractor();

    @Test
    void extractsUsefulKeywordsWithoutDuplicateWords() {
        List<String> keywords = keywordExtractor.extractKeywords(
                "Learn React React for beginners with React hooks examples and beginner projects");

        assertThat(keywords).isNotEmpty();
        assertThat(keywords)
                .allSatisfy(keyword -> {
                    String[] words = keyword.split("\\s+");
                    assertThat(words).doesNotHaveDuplicates();
                    assertThat(keyword).doesNotContain("react react");
                });
    }
}
