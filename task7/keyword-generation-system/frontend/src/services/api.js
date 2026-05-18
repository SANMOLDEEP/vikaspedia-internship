const API_BASE_URL = 'http://localhost:8080';

export const generateKeywords = async (content, maxKeywords = 20, includeLongTail = true) => {
  try {
    console.log('Sending request to:', `${API_BASE_URL}/api/keywords/generate-keywords`);
    console.log('Request payload:', { content, maxKeywords, includeLongTail });

    const response = await fetch(`${API_BASE_URL}/api/keywords/generate-keywords`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({
        content: content,
        maxKeywords: maxKeywords,
        includeLongTail: includeLongTail
      })
    });

    console.log('Response status:', response.status);

    if (!response.ok) {
      let errorMessage = `HTTP error! status: ${response.status}`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
        console.error('Error response:', errorData);
      } catch (parseError) {
        console.error('Could not parse error response:', parseError);
      }
      throw new Error(errorMessage);
    }

    const data = await response.json();
    console.log('Response data:', data);
    return data;
  } catch (error) {
    console.error('API Error:', error);
    
    // Provide more specific error messages
    if (error.name === 'TypeError' && error.message.includes('fetch')) {
      throw new Error('Unable to connect to the backend server. Please ensure the backend is running on http://localhost:8080');
    }
    
    throw error;
  }
};

export const getKeywordsByContentId = async (contentId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/keywords/content/${contentId}`);
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error('API Error:', error);
    throw error;
  }
};

export const searchKeywords = async (term) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/keywords/search?term=${encodeURIComponent(term)}`);
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error('API Error:', error);
    throw error;
  }
};

export const getPipelineMetrics = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/keywords/metrics`);
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error('API Error:', error);
    throw error;
  }
};

export const getHealthStatus = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/keywords/health`);
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error('API Error:', error);
    throw error;
  }
};

export const prefetchTrendingKeywords = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/admin/trending/prefetch`, {
      method: 'POST',
      headers: {
        'Accept': 'application/json',
      },
    });

    if (!response.ok) {
      let errorMessage = `HTTP error! status: ${response.status}`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.message || errorMessage;
      } catch (parseError) {
        console.error('Could not parse error response:', parseError);
      }
      throw new Error(errorMessage);
    }

    return await response.json();
  } catch (error) {
    console.error('Trending prefetch API Error:', error);
    throw error;
  }
};

export const getKeywordSuggestions = async (query, limit = 5) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/keywords/suggest?query=${encodeURIComponent(query)}&limit=${limit}`);

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Suggest API Error:', error);
    throw error;
  }
};

export const clusterKeywords = async (content) => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/keywords/cluster?content=${encodeURIComponent(content)}`);

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Cluster API Error:', error);
    throw error;
  }
};

export const getSearchAnalytics = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/keywords/analytics/stats`);

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Analytics API Error:', error);
    throw error;
  }
};
