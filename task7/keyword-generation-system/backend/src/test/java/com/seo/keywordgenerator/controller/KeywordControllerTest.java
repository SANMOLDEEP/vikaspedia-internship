package com.seo.keywordgenerator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seo.keywordgenerator.dto.KeywordRequestDTO;
import com.seo.keywordgenerator.dto.KeywordResponseDTO;
import com.seo.keywordgenerator.service.KeywordClusteringService;
import com.seo.keywordgenerator.service.KeywordService;
import com.seo.keywordgenerator.service.SearchAnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KeywordControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void generateKeywordsReturnsValidationMetadataContract() throws Exception {
        KeywordService keywordService = mock(KeywordService.class);
        KeywordResponseDTO keyword = new KeywordResponseDTO("react tutorial", 74.2, "MEDIUM", 120000);
        keyword.setEstimated(false);
        keyword.setSource("PYTRENDS_REDIS");
        keyword.setValidationStatus("HIGH_DEMAND_TRENDING_UP");
        keyword.setTrendDirection(0.2);
        keyword.setProcessingTimeMs(42);
        keyword.setValidatedAt(LocalDateTime.now());

        when(keywordService.generateKeywords(any(KeywordRequestDTO.class))).thenReturn(List.of(keyword));

        SearchAnalyticsService searchAnalyticsService = mock(SearchAnalyticsService.class);
        KeywordClusteringService keywordClusteringService = mock(KeywordClusteringService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new KeywordController(keywordService, searchAnalyticsService, keywordClusteringService))
                .build();

        KeywordRequestDTO request = new KeywordRequestDTO();
        request.setContent("Learn React with tutorial examples");
        request.setMaxKeywords(5);

        mockMvc.perform(post("/api/keywords/generate-keywords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].keyword").value("react tutorial"))
                .andExpect(jsonPath("$[0].source").value("PYTRENDS_REDIS"))
                .andExpect(jsonPath("$[0].validationStatus").value("HIGH_DEMAND_TRENDING_UP"))
                .andExpect(jsonPath("$[0].estimated").value(false))
                .andExpect(jsonPath("$[0].trendDirection").value(0.2))
                .andExpect(jsonPath("$[0].processingTimeMs").value(42));
    }
}
