package com.seo.keywordgenerator.enums;

/**
 * Enumeration for keyword validation status.
 * Provides clean, type-safe validation status values.
 */
public enum ValidationStatus {
    /**
     * Keyword is valid with high confidence data sources
     */
    VALID,
    
    /**
     * Keyword is valid but based on estimated/heuristic data
     */
    ESTIMATED,
    
    /**
     * Keyword failed validation criteria
     */
    REJECTED,
    
    /**
     * Keyword has low confidence in its data
     */
    LOW_CONFIDENCE,
    
    /**
     * Keyword was retrieved from cache
     */
    CACHE_HIT,
    
    /**
     * Keyword has low popularity score
     */
    LOW_POPULARITY,
    
    /**
     * Keyword has low search volume
     */
    LOW_VOLUME,
    
    /**
     * Keyword has low demand score
     */
    LOW_DEMAND,
    
    /**
     * Keyword input is invalid
     */
    INVALID_INPUT
}
