import React, { useRef, useEffect } from 'react';

const ContentInput = ({ content, setContent, onGenerate, isLoading, minChars = 10, maxChars = 10000 }) => {
  const textareaRef = useRef(null);

  // Auto-resize textarea based on content
  useEffect(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      // Reset height to auto to get the correct scrollHeight
      textarea.style.height = 'auto';
      // Set height to scrollHeight, but ensure minimum height
      const scrollHeight = textarea.scrollHeight;
      textarea.style.height = Math.max(80, scrollHeight) + 'px';
    }
  }, [content]);

  const handleContentChange = (e) => {
    const value = e.target.value;
    if (value.length <= maxChars) {
      setContent(value);
    }
  };

  const handleGenerate = () => {
    if (content.trim().length >= minChars && !isLoading) {
      onGenerate();
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && e.ctrlKey) {
      handleGenerate();
    }
  };

  const isValid = content.trim().length >= minChars;
  const isAtLimit = content.length >= maxChars;

  return (
    <div className="textarea-container">
      <textarea
        ref={textareaRef}
        value={content}
        onChange={handleContentChange}
        onKeyPress={handleKeyPress}
        placeholder="Enter your content here... For example: 'Learn React for beginners with step by step tutorial and examples'"
        className={`content-textarea ${isAtLimit ? 'at-limit' : ''}`}
        rows={1}
        disabled={isLoading}
      />
      
      <div className="input-footer">
        <div className="character-count">
          <span className={isAtLimit ? 'warning' : ''}>
            {content.length} / {maxChars} characters
          </span>
          {isAtLimit && (
            <span className="warning-text">Maximum character limit reached</span>
          )}
        </div>
        
        <div className="input-actions">
          <button
            onClick={handleGenerate}
            disabled={!isValid || isLoading}
            className={`generate-button ${!isValid || isLoading ? 'disabled' : ''}`}
          >
            {isLoading ? 'Generating...' : 'Generate Keywords'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ContentInput;