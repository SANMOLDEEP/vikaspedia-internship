package com.seo.keywordgenerator.service;

import com.seo.keywordgenerator.dto.KeywordValidationResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordValidatorTest {

    @Test
    void marksFallbackVolumeAsEstimatedAndDeterministic() {
        KeywordValidator validator = new KeywordValidator(new EmptyTrendService());

        KeywordValidationResult first = validator.validateKeyword("react tutorial for beginners");
        KeywordValidationResult second = validator.validateKeyword("react tutorial for beginners");

        assertThat(first.isEstimated()).isTrue();
        assertThat(first.getSource()).isEqualTo("ESTIMATED_FALLBACK");
        assertThat(first.getSearchVolume()).isEqualTo(second.getSearchVolume());
        assertThat(first.getValidationStatus()).startsWith("ESTIMATED");
    }

    @Test
    void preservesRealTrendSourceAndTimestampMetadata() {
        KeywordValidator validator = new KeywordValidator(new StaticTrendService());

        KeywordValidationResult result = validator.validateKeyword("react tutorial");

        assertThat(result.isEstimated()).isFalse();
        assertThat(result.getSource()).isEqualTo("PYTRENDS_REDIS");
        assertThat(result.getValidatedAt()).isNotNull();
        assertThat(result.getTrendDirection()).isGreaterThan(0);
    }

    private static class EmptyTrendService implements TrendService {
        @Override
        public Optional<TrendData> getTrendData(String keyword) {
            return Optional.empty();
        }

        @Override
        public Optional<TrendData> getTrendData(String keyword, LocalDateTime startDate, LocalDateTime endDate) {
            return Optional.empty();
        }

        @Override
        public Map<String, TrendData> getBatchTrendData(List<String> keywords) {
            return Collections.emptyMap();
        }

        @Override
        public boolean isTrending(String keyword) {
            return false;
        }

        @Override
        public double getPopularityScore(String keyword) {
            return 0;
        }

        @Override
        public double getTrendScore(String keyword) {
            return 55;
        }

        @Override
        public void cacheTrendData(String keyword, TrendData trendData) {
        }

        @Override
        public void invalidateCache(String keyword) {
        }
    }

    private static final class StaticTrendService extends EmptyTrendService {
        @Override
        public Optional<TrendData> getTrendData(String keyword) {
            return Optional.of(new TrendData(
                    keyword,
                    75.0,
                    120000L,
                    0.2,
                    LocalDateTime.now(),
                    Map.of("US", 60000L),
                    Map.of("react guide", 0.9),
                    "PYTRENDS_REDIS",
                    false
            ));
        }
    }
}
