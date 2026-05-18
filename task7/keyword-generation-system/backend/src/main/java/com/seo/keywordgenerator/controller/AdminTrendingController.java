package com.seo.keywordgenerator.controller;

import com.seo.keywordgenerator.scheduler.TrendingKeywordScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/trending")
@RequiredArgsConstructor
@Slf4j
public class AdminTrendingController {

    private final TrendingKeywordScheduler trendingKeywordScheduler;

    /**
     * Manual trigger for trending keyword fetch + Pytrends prefetch.
     * Useful for local testing instead of waiting for the daily 02:00 scheduler.
     */
    @PostMapping("/prefetch")
    public ResponseEntity<Map<String, Object>> prefetch() {
        log.info("Manual admin trigger: trending prefetch");
        TrendingKeywordScheduler.TrendingPrefetchResult result = trendingKeywordScheduler.triggerTrendingKeywordsFetch();

        return ResponseEntity.ok(Map.of(
                "status", result.status(),
                "cachedKeywords", result.cachedKeywords(),
                "pytrendsKeywords", result.pytrendsKeywords(),
                "message", result.message()
        ));
    }
}

