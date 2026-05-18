import { useState, useEffect } from 'react';
import ContentInput from './components/ContentInput';
import Loader from './components/Loader';
import KeywordList from './components/KeywordList';
import {
  clusterKeywords,
  generateKeywords,
  getHealthStatus,
  getKeywordSuggestions,
  getSearchAnalytics,
  prefetchTrendingKeywords
} from './services/api';
import './App.css';

function App() {
  const [content, setContent] = useState('');
  const [keywords, setKeywords] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [processingTime, setProcessingTime] = useState(null);
  const [loaderProgress, setLoaderProgress] = useState(0);
  const [backendStatus, setBackendStatus] = useState('unknown'); // 'connected', 'disconnected', 'unknown'
  const [prefetchStatus, setPrefetchStatus] = useState(null);
  const [isPrefetching, setIsPrefetching] = useState(false);
  const [suggestQuery, setSuggestQuery] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const [isSuggesting, setIsSuggesting] = useState(false);
  const [clusterResult, setClusterResult] = useState(null);
  const [isClustering, setIsClustering] = useState(false);
  const [analytics, setAnalytics] = useState(null);
  const [isLoadingAnalytics, setIsLoadingAnalytics] = useState(false);
  const [bonusError, setBonusError] = useState(null);

  // Check backend health on component mount
  useEffect(() => {
    const checkBackendHealth = async () => {
      try {
        await getHealthStatus();
        setBackendStatus('connected');
      } catch (error) {
        console.log('Backend health check failed:', error.message);
        setBackendStatus('disconnected');
      }
    };

    checkBackendHealth();
  }, []);

  const handleGenerate = async () => {
    setIsLoading(true);
    setError(null);
    setLoaderProgress(0);
    setKeywords([]);

    // Simulate progress updates
    const progressInterval = setInterval(() => {
      setLoaderProgress(prev => {
        if (prev >= 90) {
          clearInterval(progressInterval);
          return 90;
        }
        return prev + Math.random() * 15;
      });
    }, 300);

    try {
      const startTime = Date.now();
      console.log('Starting keyword generation for content:', content.substring(0, 100) + '...');
      
      const result = await generateKeywords(content, 20, true);
      const endTime = Date.now();

      console.log('Keywords generated successfully:', result);
      console.log('Processing time:', endTime - startTime, 'ms');

      // Ensure we have an array of keyword objects
      if (Array.isArray(result)) {
        setKeywords(result);
        setProcessingTime(endTime - startTime);
        setLoaderProgress(100);
      } else {
        throw new Error('Invalid response format from server');
      }
    } catch (err) {
      console.error('Error generating keywords:', err);
      setError(err.message || 'Failed to generate keywords. Please try again.');
    } finally {
      clearInterval(progressInterval);
      setTimeout(() => {
        setIsLoading(false);
        setLoaderProgress(0);
      }, 500);
    }
  };

  const handleTrendingPrefetch = async () => {
    setIsPrefetching(true);
    setPrefetchStatus(null);

    try {
      const result = await prefetchTrendingKeywords();
      setPrefetchStatus(result);
      setBackendStatus('connected');
    } catch (err) {
      setPrefetchStatus({
        status: 'failed',
        message: err.message || 'Trending prefetch failed.',
      });
    } finally {
      setIsPrefetching(false);
    }
  };

  const handleSuggest = async () => {
    if (!suggestQuery.trim()) return;

    setIsSuggesting(true);
    setBonusError(null);

    try {
      const result = await getKeywordSuggestions(suggestQuery.trim(), 6);
      setSuggestions(result.suggestions || []);
    } catch (err) {
      setBonusError(err.message || 'Unable to load suggestions.');
    } finally {
      setIsSuggesting(false);
    }
  };

  const handleCluster = async () => {
    if (content.trim().length < 10) return;

    setIsClustering(true);
    setBonusError(null);

    try {
      const result = await clusterKeywords(content);
      setClusterResult(result);
    } catch (err) {
      setBonusError(err.message || 'Unable to cluster keywords.');
    } finally {
      setIsClustering(false);
    }
  };

  const handleAnalytics = async () => {
    setIsLoadingAnalytics(true);
    setBonusError(null);

    try {
      const result = await getSearchAnalytics();
      setAnalytics(result);
    } catch (err) {
      setBonusError(err.message || 'Unable to load analytics.');
    } finally {
      setIsLoadingAnalytics(false);
    }
  };

  return (
    <div className="app">
      <main className="app-main">
        {isLoading && (
          <Loader
            message="Analyzing content and generating keywords..."
            showProgress={true}
            progress={loaderProgress}
          />
        )}

        <section className="main-content-section">
          {/* Top Header with Status */}
          <div className="top-heading">
            <div className="title-section">
              <h1>SEO Keyword Generator</h1>
              <p className="main-subtitle">
                Generate high-quality SEO keywords from your content.
              </p>
            </div>
            <div className="status-top-right">
              <span className={`status-indicator ${backendStatus}`}>
                {backendStatus === 'connected' && 'Backend Connected'}
                {backendStatus === 'disconnected' && 'Backend Offline'}
                {backendStatus === 'unknown' && 'Checking Backend'}
              </span>
            </div>
          </div>

          {/* Tips and Admin Sections in Parallel */}
          <div className="parallel-sections">
            {/* Admin Section */}
            <div className="admin-section">
              <div className="admin-header">
                <h2>Trending Prefetch</h2>
                <p className="admin-description">
                  Initialize trending keyword cache from the backend.
                </p>
              </div>
              <button
                className="prefetch-button"
                type="button"
                onClick={handleTrendingPrefetch}
                disabled={isPrefetching || backendStatus === 'disconnected'}
              >
                {isPrefetching ? 'Running...' : 'Run Prefetch'}
              </button>
              {prefetchStatus && (
                <div className={`prefetch-result ${prefetchStatus.status}`}>
                  <strong>{prefetchStatus.status}</strong>
                  {typeof prefetchStatus.cachedKeywords === 'number' && (
                    <span>{prefetchStatus.cachedKeywords} cached keywords</span>
                  )}
                  {typeof prefetchStatus.pytrendsKeywords === 'number' && (
                    <span>{prefetchStatus.pytrendsKeywords} Pytrends candidates</span>
                  )}
                  <p>{prefetchStatus.message}</p>
                </div>
              )}
            </div>

            {/* Tips Section */}
            <div className="tips-section">
              <h3>Tips for Better Results</h3>
              <ul className="tips-list">
                <li>Provide detailed and structured content</li>
                <li>Include relevant technical terms and concepts</li>
                <li>Use proper grammar and spelling</li>
                <li>Aim for 100–500 words for optimal results</li>
              </ul>
            </div>
          </div>

          <section className="bonus-panel" aria-label="Bonus keyword tools">
            <div className="bonus-panel-header">
              <h2>Keyword Intelligence Tools</h2>
              <p>Review suggestions, keyword clusters, and internal search activity.</p>
            </div>

            <div className="bonus-grid">
              <div className="bonus-tool">
                <h3>Auto-Suggest</h3>
                <div className="inline-control">
                  <input
                    type="text"
                    value={suggestQuery}
                    onChange={(event) => setSuggestQuery(event.target.value)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter') handleSuggest();
                    }}
                    placeholder="Enter a seed term"
                    className="bonus-input"
                  />
                  <button type="button" className="secondary-button" onClick={handleSuggest} disabled={isSuggesting}>
                    {isSuggesting ? 'Loading' : 'Suggest'}
                  </button>
                </div>
                {suggestions.length > 0 && (
                  <div className="suggestion-list">
                    {suggestions.map((suggestion) => (
                      <button
                        type="button"
                        key={suggestion}
                        className="suggestion-chip"
                        onClick={() => setSuggestQuery(suggestion)}
                      >
                        {suggestion}
                      </button>
                    ))}
                  </div>
                )}
              </div>

              <div className="bonus-tool">
                <h3>Keyword Clusters</h3>
                <p className="tool-description">Group generated phrases by concept, pattern, and popularity.</p>
                <button
                  type="button"
                  className="secondary-button"
                  onClick={handleCluster}
                  disabled={isClustering || content.trim().length < 10}
                >
                  {isClustering ? 'Clustering' : 'Cluster Current Content'}
                </button>
                {clusterResult?.summary && (
                  <div className="compact-stats">
                    <span>{clusterResult.summary.totalClusters} clusters</span>
                    <span>{clusterResult.clusteredKeywords} grouped keywords</span>
                    <span>Largest: {clusterResult.summary.largestCluster}</span>
                  </div>
                )}
              </div>

              <div className="bonus-tool">
                <h3>Internal Analytics</h3>
                <p className="tool-description">Show tracked search activity from the local analytics table.</p>
                <button
                  type="button"
                  className="secondary-button"
                  onClick={handleAnalytics}
                  disabled={isLoadingAnalytics}
                >
                  {isLoadingAnalytics ? 'Loading' : 'Load Analytics'}
                </button>
                {analytics && (
                  <div className="analytics-summary">
                    <span>Total recent searches: {analytics.totalSearches ?? 0}</span>
                    {analytics.topKeywords?.length > 0 && (
                      <span>Top: {analytics.topKeywords.slice(0, 3).join(', ')}</span>
                    )}
                  </div>
                )}
              </div>
            </div>

            {clusterResult?.clusters && Object.keys(clusterResult.clusters).length > 0 && (
              <div className="cluster-preview">
                {Object.entries(clusterResult.clusters).slice(0, 4).map(([clusterName, clusterKeywords]) => (
                  <div className="cluster-group" key={clusterName}>
                    <h4>{clusterName}</h4>
                    <p>{clusterKeywords.slice(0, 3).map((keyword) => keyword.keyword).join(', ')}</p>
                  </div>
                ))}
              </div>
            )}

            {bonusError && <p className="bonus-error">{bonusError}</p>}
          </section>

          {/* Content Area */}
          <div className="content-header">
            <h2>Enter Your Content</h2>
            <p className="content-description">
              Provide text content to generate SEO-optimized keywords.
              Minimum 10 characters required.
            </p>
          </div>

          <ContentInput
            content={content}
            setContent={setContent}
            onGenerate={handleGenerate}
            isLoading={isLoading}
          />
        </section>

        {(keywords.length > 0 || error) && (
          <div className="results-section">
            <KeywordList
              keywords={keywords}
              isLoading={isLoading}
              error={error}
              processingTime={processingTime}
            />
          </div>
        )}
      </main>
      
      <footer className="app-footer">
        <p>SEO Keyword Generator. Built with React & Spring Boot.</p>
      </footer>
    </div>
  );
}

export default App;
