import React from 'react';

const PopularityIndicator = ({ score, showLabel = true }) => {
  const getPopularityLevel = (score) => {
    if (score >= 80) return { level: 'high', color: '#28a745', label: 'High' };
    if (score >= 60) return { level: 'medium', color: '#ffc107', label: 'Medium' };
    if (score >= 40) return { level: 'low', color: '#fd7e14', label: 'Low' };
    return { level: 'very-low', color: '#dc3545', label: 'Very Low' };
  };

  const popularity = getPopularityLevel(score);
  const normalizedScore = Math.max(0, Math.min(100, Number(score) || 0));

  return (
    <div className="popularity-indicator">
      <div className="popularity-bar">
        <div className="popularity-fill" style={{ width: `${normalizedScore}%`, backgroundColor: popularity.color }}></div>
      </div>
      {showLabel && (
        <span className="popularity-label" style={{ color: popularity.color }}>
          {popularity.label} Popularity
        </span>
      )}
    </div>
  );
};

export default PopularityIndicator;
