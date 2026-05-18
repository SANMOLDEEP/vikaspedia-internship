package com.seo.keywordgenerator.config;

import com.seo.keywordgenerator.util.RAKEExtractor;
import com.seo.keywordgenerator.util.TextPreprocessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeywordGeneratorConfig {
    
    private final RAKEExtractor rakeExtractor;
    
    public KeywordGeneratorConfig(RAKEExtractor rakeExtractor) {
        this.rakeExtractor = rakeExtractor;
    }
    
    @Bean
    public com.seo.keywordgenerator.util.KeywordGeneratorEngineNew keywordGeneratorEngineNew(TextPreprocessor textPreprocessor) {
        return new com.seo.keywordgenerator.util.KeywordGeneratorEngineNew(textPreprocessor, rakeExtractor);
    }
}
