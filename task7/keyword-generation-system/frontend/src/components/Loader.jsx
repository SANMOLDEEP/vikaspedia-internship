import React from 'react';

const Loader = ({ message = "Processing...", showProgress = false, progress = 0 }) => {
  return (
    <div className="loader-container">
      <div className="loader-content">
        <div className="loader-spinner">
          <div className="spinner"></div>
          <div className="spinner-text">{message}</div>
        </div>
        
        {showProgress && (
          <div className="progress-container">
            <div className="progress-bar">
              <div 
                className="progress-fill" 
                style={{ width: `${Math.min(progress, 100)}%` }}
              ></div>
            </div>
            <div className="progress-text">{Math.round(progress)}%</div>
          </div>
        )}
        
        <div className="loader-steps">
          <div className={`step ${progress >= 20 ? 'active' : ''}`}>
            <div className="step-icon">1</div>
            <div className="step-label">Preprocessing</div>
          </div>
          <div className={`step ${progress >= 40 ? 'active' : ''}`}>
            <div className="step-icon">2</div>
            <div className="step-label">Extracting</div>
          </div>
          <div className={`step ${progress >= 60 ? 'active' : ''}`}>
            <div className="step-icon">3</div>
            <div className="step-label">Generating</div>
          </div>
          <div className={`step ${progress >= 80 ? 'active' : ''}`}>
            <div className="step-icon">4</div>
            <div className="step-label">Optimizing</div>
          </div>
          <div className={`step ${progress >= 100 ? 'active' : ''}`}>
            <div className="step-icon">5</div>
            <div className="step-label">Validating</div>
          </div>
        </div>
        
        <div className="loader-info">
          <p>Please wait while we analyze your content and generate the best SEO keywords...</p>
          <p className="info-small">This usually takes a few seconds.</p>
        </div>
      </div>
    </div>
  );
};

export default Loader;