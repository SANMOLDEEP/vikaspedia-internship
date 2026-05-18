package com.seo.keywordgenerator.service;

import com.seo.keywordgenerator.dto.KeywordRequestDTO;
import com.seo.keywordgenerator.dto.KeywordResponseDTO;
import com.seo.keywordgenerator.scheduler.TrendingKeywordScheduler;
import com.seo.keywordgenerator.util.KeywordExtractor;
import com.seo.keywordgenerator.util.KeywordGeneratorEngineNew;
import com.seo.keywordgenerator.util.RAKEExtractor;
import com.seo.keywordgenerator.util.SEOOptimizer;
import com.seo.keywordgenerator.util.TextPreprocessor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KeywordGenerationPipelineTest {

    @Test
    void returnsCachedKeywordsWithoutRunningExtraction() {
        CacheService cacheService = mock(CacheService.class);
        TextPreprocessor textPreprocessor = mock(TextPreprocessor.class);
        KeywordExtractor keywordExtractor = mock(KeywordExtractor.class);
        RAKEExtractor rakeExtractor = mock(RAKEExtractor.class);
        KeywordGeneratorEngineNew keywordGenerator = mock(KeywordGeneratorEngineNew.class);
        SEOOptimizer seoOptimizer = mock(SEOOptimizer.class);
        KeywordValidator validator = mock(KeywordValidator.class);
        TrendingKeywordScheduler scheduler = mock(TrendingKeywordScheduler.class);

        KeywordResponseDTO cached = new KeywordResponseDTO("react tutorial", 72.0, "MEDIUM", 120000);
        cached.setSource("PYTRENDS_REDIS");
        when(cacheService.isContentProcessed("Learn React fast")).thenReturn(true);
        when(cacheService.getCachedKeywords("Learn React fast")).thenReturn(List.of(cached));

        KeywordGenerationPipeline pipeline = new KeywordGenerationPipeline(
                textPreprocessor,
                keywordExtractor,
                rakeExtractor,
                keywordGenerator,
                seoOptimizer,
                validator,
                cacheService,
                scheduler
        );

        KeywordRequestDTO request = new KeywordRequestDTO();
        request.setContent("Learn React fast");

        KeywordGenerationPipeline.PipelineResult result = pipeline.generateKeywords(request);

        assertThat(result.getStatus()).isEqualTo("DUPLICATE_CONTENT_CACHE_HIT");
        assertThat(result.getKeywords()).hasSize(1);
        assertThat(result.getKeywords().get(0).getProcessingTimeMs()).isGreaterThanOrEqualTo(0);
        verifyNoInteractions(textPreprocessor, keywordExtractor, rakeExtractor, keywordGenerator, seoOptimizer, validator, scheduler);
    }
}
