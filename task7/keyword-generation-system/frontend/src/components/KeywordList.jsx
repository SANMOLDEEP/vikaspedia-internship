import React, { useState } from 'react';
import PopularityIndicator from './PopularityIndicator';

const KeywordList = ({ keywords, isLoading, error, processingTime }) => {
  const [sortBy, setSortBy] = useState('score');
  const [filterType, setFilterType] = useState('all');
  const [expandedKeywords, setExpandedKeywords] = useState(new Set());

  if (isLoading) {
    return (
      <div className="keyword-list loading">
        <div className="loading-placeholder">
          <div className="placeholder-text">Generating keywords...</div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="keyword-list error">
        <div className="error-container">
          <h3>Error</h3>
          <p>{error}</p>
          <button onClick={() => window.location.reload()} className="retry-button">
            Try Again
          </button>
        </div>
      </div>
    );
  }

  if (!keywords || keywords.length === 0) {
    return (
      <div className="keyword-list empty">
        <div className="empty-state">
          <h3>No Keywords Generated</h3>
          <p>Please check your content and try again.</p>
        </div>
      </div>
    );
  }

  const getFilteredAndSortedKeywords = () => {
    let filtered = keywords;

    if (filterType !== 'all') {
      filtered = keywords.filter((keyword) => keyword.type === filterType);
    }

    return filtered.sort((a, b) => {
      switch (sortBy) {
        case 'score':
          return b.score - a.score;
        case 'searchVolume':
          return (b.searchVolume || 0) - (a.searchVolume || 0);
        case 'alphabetical':
          return a.keyword.localeCompare(b.keyword);
        case 'length':
          return b.keyword.length - a.keyword.length;
        default:
          return b.score - a.score;
      }
    });
  };

  const toggleKeywordExpansion = (keyword) => {
    const newExpanded = new Set(expandedKeywords);
    if (newExpanded.has(keyword)) {
      newExpanded.delete(keyword);
    } else {
      newExpanded.add(keyword);
    }
    setExpandedKeywords(newExpanded);
  };

  const getTypeClass = (type) => {
    switch (type) {
      case 'SHORT':
        return 'badge-short';

      case 'MEDIUM':
        return 'badge-medium';

      case 'LONG_TAIL':
        return 'badge-long';

      default:
        return 'badge-default';
    }
  };

  const getTypeLabel = (type) => {
    switch (type) {
      case 'SHORT':
        return 'Short';
      case 'MEDIUM':
        return 'Medium';
      case 'LONG_TAIL':
        return 'Long-tail';
      default:
        return type;
    }
  };

  const filteredKeywords = getFilteredAndSortedKeywords();
  const uniqueTypes = [...new Set(keywords.map((k) => k.type))];

  return (
    <div className="keyword-list">
      <div className="list-header">
        <h2>Generated Keywords</h2>
        {processingTime && <p className="processing-time">Generated in {processingTime}ms</p>}
      </div>

      <div className="list-controls">
        <div className="control-group">
          <label>Sort by:</label>
          <select value={sortBy} onChange={(e) => setSortBy(e.target.value)} className="sort-select">
            <option value="score">Score</option>
            <option value="searchVolume">Search Volume</option>
            <option value="alphabetical">Alphabetical</option>
            <option value="length">Length</option>
          </select>
        </div>

        <div className="control-group">
          <label>Filter by type:</label>
          <select value={filterType} onChange={(e) => setFilterType(e.target.value)} className="filter-select">
            <option value="all">All Types</option>
            {uniqueTypes.map((type) => (
              <option key={type} value={type}>
                {getTypeLabel(type)} ({keywords.filter((k) => k.type === type).length})
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="keyword-stats">
        <div className="stat-item">
          <span className="stat-label">Total Keywords:</span>
          <span className="stat-value">{keywords.length}</span>
        </div>
        <div className="stat-item">
          <span className="stat-label">Filtered:</span>
          <span className="stat-value">{filteredKeywords.length}</span>
        </div>
        <div className="stat-item">
          <span className="stat-label">Avg Score:</span>
          <span className="stat-value">{(keywords.reduce((sum, k) => sum + k.score, 0) / keywords.length).toFixed(1)}</span>
        </div>
      </div>

      <div className="keywords-container">
        {filteredKeywords.map((keyword, index) => (
          <div key={index} className="keyword-item">
            <div className="keyword-header">
              <h3 className="keyword-text">{keyword.keyword}</h3>
              <div className="keyword-badges">
                <span className={`type-badge ${getTypeClass(keyword.type)}`}>{getTypeLabel(keyword.type)}</span>
                <PopularityIndicator score={keyword.score} />
              </div>
            </div>

            <div className="keyword-details">
              <div className="detail-row">
                <span className="detail-label">Score:</span>
                <span className="detail-value">{typeof keyword.score === 'number' ? keyword.score.toFixed(2) : 'N/A'}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Search Volume:</span>
                <span className="detail-value">
                  {keyword.searchVolume
                    ? `${keyword.searchVolume.toLocaleString()}${keyword.estimated ? ' (estimated)' : ''}`
                    : keyword.source === 'GOOGLE_AUTOCOMPLETE'
                      ? 'Verified by autocomplete'
                      : 'N/A'}
                </span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Source:</span>
                <span className="detail-value">{keyword.source || 'N/A'}</span>
              </div>
              <div className="detail-row">
                <span className="detail-label">Validation:</span>
                <span className="detail-value">{keyword.validationStatus || 'N/A'}</span>
              </div>
            </div>

            <div className="keyword-actions">
              <button onClick={() => toggleKeywordExpansion(keyword.keyword)} className="expand-button">
                {expandedKeywords.has(keyword.keyword) ? 'Show Less' : 'Show More'}
              </button>
              <button
                onClick={() => navigator.clipboard.writeText(keyword.keyword)}
                className="copy-button"
                title="Copy keyword"
              >
                Copy
              </button>
            </div>

            {expandedKeywords.has(keyword.keyword) && (
              <div className="keyword-expanded">
                <div className="expanded-content">
                  <h4>Keyword Analysis</h4>
                  <div className="analysis-grid">
                    <div className="analysis-item">
                      <span className="analysis-label">Word Count:</span>
                      <span className="analysis-value">{keyword.keyword.split(' ').length}</span>
                    </div>
                    <div className="analysis-item">
                      <span className="analysis-label">Character Count:</span>
                      <span className="analysis-value">{keyword.keyword.length}</span>
                    </div>
                    <div className="analysis-item">
                      <span className="analysis-label">Type:</span>
                      <span className="analysis-value">{getTypeLabel(keyword.type)}</span>
                    </div>
                    <div className="analysis-item">
                      <span className="analysis-label">Quality:</span>
                      <span className="analysis-value">
                        {keyword.score >= 80 ? 'Excellent' : keyword.score >= 60 ? 'Good' : 'Fair'}
                      </span>
                    </div>
                    <div className="analysis-item">
                      <span className="analysis-label">Trend Direction:</span>
                      <span className="analysis-value">
                        {typeof keyword.trendDirection === 'number' ? keyword.trendDirection.toFixed(2) : 'N/A'}
                      </span>
                    </div>
                    <div className="analysis-item">
                      <span className="analysis-label">Processing Time:</span>
                      <span className="analysis-value">{keyword.processingTimeMs ? `${keyword.processingTimeMs}ms` : 'N/A'}</span>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>

      {filteredKeywords.length === 0 && (
        <div className="no-results">
          <p>No keywords match the current filter criteria.</p>
        </div>
      )}
    </div>
  );
};

export default KeywordList;

