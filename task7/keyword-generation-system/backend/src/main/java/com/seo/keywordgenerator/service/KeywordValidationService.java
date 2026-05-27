package com.seo.keywordgenerator.service;

import com.seo.keywordgenerator.dto.KeywordResponseDTO;
import com.seo.keywordgenerator.dto.KeywordValidationResult;
import com.seo.keywordgenerator.repository.KeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeywordValidationService {

    private final KeywordValidator keywordValidator;
    private final KeywordRepository keywordRepository;
    private final KeywordRankingService keywordRankingService;

    public List<KeywordResponseDTO> validateAndEnrich(List<KeywordRankingService.RankedKeyword> rankedKeywords,
                                                       KeywordExtractionService.ExtractionResult extraction) {
        if (rankedKeywords == null || rankedKeywords.isEmpty()) {
            return List.of();
        }

        List<String> keywordStrings = rankedKeywords.stream()
                .map(ranked -> ranked.candidate().keyword())
                .toList();
        Map<String, KeywordValidationResult> validationByKeyword = keywordValidator
                .validateKeywordsWithBatchProcessing(keywordStrings)
                .stream()
                .collect(Collectors.toMap(
                        result -> normalize(result.getKeyword()),
                        result -> result,
                        (first, ignored) -> first
                ));

        List<KeywordResponseDTO> enriched = new ArrayList<>();
        for (KeywordRankingService.RankedKeyword ranked : rankedKeywords) {
            String keyword = ranked.candidate().keyword();
            KeywordValidationResult validation = validationByKeyword.get(normalize(keyword));
            if (validation == null || keywordRankingService.isInvalidPhrase(keyword)) {
                continue;
            }

            double confidence = confidenceScore(ranked, validation, extraction);
            if (!validation.isValid() && confidence < 45.0) {
                continue;
            }

            KeywordResponseDTO dto = new KeywordResponseDTO(keyword, finalScore(ranked, validation, confidence), ranked.candidate().type());
            dto.setSearchVolume((int) Math.max(0L, validation.getSearchVolume()));
            dto.setEstimated(validation.isEstimated());
            dto.setSource(validation.getSource());
            dto.setValidationStatus(validationStatus(validation, confidence));
            dto.setTrendDirection(validation.getTrendDirection());
            dto.setValidatedAt(validation.getValidatedAt() != null ? validation.getValidatedAt() : LocalDateTime.now());
            dto.setConfidenceScore(confidence);
            dto.setSearchIntent(ranked.candidate().searchIntent());
            dto.setPopularityTier(popularityTier(dto.getScore(), validation));
            enriched.add(dto);
        }

        return enriched.stream()
                .sorted(Comparator.comparingDouble(KeywordResponseDTO::getScore).reversed())
                .toList();
    }

    private double finalScore(KeywordRankingService.RankedKeyword ranked, KeywordValidationResult validation, double confidence) {
        double trendSignal = validation.getPopularityScore();
        double sourceBoost = sourceBoost(validation.getSource());
        double score = ranked.score() * 0.58 + confidence * 0.22 + trendSignal * 0.15 + sourceBoost;
        return clamp(round(score), 0.0, 100.0);
    }

    private double confidenceScore(KeywordRankingService.RankedKeyword ranked, KeywordValidationResult validation,
                                   KeywordExtractionService.ExtractionResult extraction) {
        double score = 34.0;
        score += ranked.naturalnessScore() * 0.25;
        score += ranked.score() * 0.20;
        score += sourceBoost(validation.getSource());
        if (validation.isValid()) score += 10.0;
        if (extraction.frequencyFor(ranked.candidate().keyword()) > 0) score += 8.0;
        if (internalKeywordExists(ranked.candidate().keyword())) score += 6.0;
        if ("PYTRENDS_REDIS".equals(validation.getSource())) score += 8.0;
        if ("GOOGLE_AUTOCOMPLETE".equals(validation.getSource())) score += 14.0;
        if ("NO_TREND_DATA".equals(validation.getSource())) score -= 15.0;
        return clamp(Math.round(score), 0.0, 100.0);
    }

    private boolean internalKeywordExists(String keyword) {
        try {
            return keywordRepository.countByKeywordIgnoreCase(keyword) > 0;
        } catch (Exception e) {
            log.debug("Internal keyword frequency lookup failed for '{}': {}", keyword, e.getMessage());
            return false;
        }
    }

    private double sourceBoost(String source) {
        if ("GOOGLE_AUTOCOMPLETE".equals(source)) return 14.0;
        if ("PYTRENDS_REDIS".equals(source)) return 18.0;
        if ("ESTIMATED_FALLBACK".equals(source)) return 3.0;
        return 0.0;
    }

    private String validationStatus(KeywordValidationResult validation, double confidence) {
        if (validation.isValid() && confidence >= 55.0) return "VALIDATED";
        if (validation.isValid()) return "WEAKLY_VALIDATED";
        if (confidence >= 60.0) return "QUALITY_VALIDATED";
        return validation.getValidationStatus();
    }

    private String popularityTier(double score, KeywordValidationResult validation) {
        double blended = Math.max(score, validation.getPopularityScore());
        if (blended >= 80.0) return "HIGH";
        if (blended >= 60.0) return "MEDIUM";
        return "LOW";
    }

    private String normalize(String keyword) {
        return keyword == null ? "" : keyword.toLowerCase(Locale.ROOT).trim();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
