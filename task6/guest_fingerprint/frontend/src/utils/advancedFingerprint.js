import FingerprintJS from '@fingerprintjs/fingerprintjs';

// API Base URL - configurable for different environments
const API_BASE_URL = window.location.hostname === 'localhost' 
  ? 'http://localhost:8080' 
  : `http://${window.location.hostname}:8080`;

let fingerprint = null;
let fingerprintPromise = null;
let sessionPromise = null;

export const getAdvancedFingerprint = async () => {
  // Return cached result if already available
  if (fingerprint) {
    return {
      visitorId: fingerprint,
      confidence: { score: 0.9 },
      components: {}
    };
  }

  // Return existing promise if already loading
  if (fingerprintPromise) {
    return fingerprintPromise;
  }

  // Create and cache the promise
  fingerprintPromise = (async () => {
    try {
      // Load FingerprintJS
      const fp = await FingerprintJS.load({
        apiKey: 'your-api-key', // Optional: Get from FingerprintJS
        region: 'eu' // Optional: Choose region
      });

      // Get the visitor identifier
      const result = await fp.get();
      fingerprint = result.visitorId;
      
      // Get detailed components for analysis
      const components = result.components || {};
      
      return {
        visitorId: fingerprint,
        confidence: result.confidence,
        components: components
      };
      
    } catch (error) {
      console.error('Fingerprint generation error:', error);
      // Fallback fingerprint
      fingerprint = 'fallback_' + Math.random().toString(36).substr(2, 9);
      return {
        visitorId: fingerprint,
        confidence: { score: 0.1 },
        components: {},
        error: true
      };
    }
  })();

  return fingerprintPromise;
};

export const startSession = async (botData = null) => {
  // Return existing promise if already loading
  if (sessionPromise) {
    return sessionPromise;
  }

  // Create and cache the promise
  sessionPromise = (async () => {
    try {
      const fp = await getAdvancedFingerprint();
      const sessionId = 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
      
      // Create session in backend with actual client data and bot detection
      const response = await fetch(`${API_BASE_URL}/api/sessions`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          fingerprintId: fp.visitorId,
          sessionId: sessionId,
          startTime: new Date().toISOString(),
          ipAddress: '127.0.0.1', // Will be replaced by actual IP on server
          userAgent: navigator.userAgent, // Actual browser user agent
          isBot: botData ? botData.isBot : false,
          indicators: botData ? botData.indicators : null
        })
      });
      
      const sessionData = await response.json();
      
      return {
        sessionId: sessionData.sessionId || sessionId,
        fingerprintId: fp.visitorId,
        status: sessionData.status || 'created',
        botScore: sessionData.botScore,
        bot: sessionData.bot,
        indicators: sessionData.indicators,
        totalVisits: sessionData.totalVisits,
        message: sessionData.message
      };
      
    } catch (error) {
      console.error('Session creation error:', error);
      // Fallback session
      return {
        sessionId: 'fallback_session_' + Date.now(),
        fingerprintId: 'fallback_fingerprint',
        status: 'fallback'
      };
    }
  })();

  return sessionPromise;
};

export const trackEvent = async (eventType, eventData = {}) => {
  try {
    const fp = await getAdvancedFingerprint();
    
    const response = await fetch(`${API_BASE_URL}/api/events`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        fingerprintId: fp.visitorId,
        eventType: eventType,
        eventData: eventData,
        timestamp: new Date().toISOString(),
        ipAddress: '127.0.0.1', // Will be replaced by actual IP on server
        userAgent: navigator.userAgent // Actual browser user agent
      })
    });
    
    const result = await response.json();
    return result;
    
  } catch (error) {
    console.error('Event tracking error:', error);
    return { success: false, error: error.message };
  }
};

export const detectBot = () => {
  const userAgent = navigator.userAgent;
  const indicators = [];
  let botScore = 0;
  
  // Check for common bot indicators
  if (userAgent.includes('bot') || userAgent.includes('crawler') || userAgent.includes('spider')) {
    indicators.push('Bot-like user agent');
    botScore += 0.7;
  }
  
  if (userAgent.includes('headless') || userAgent.includes('phantom') || userAgent.includes('selenium')) {
    indicators.push('Headless browser detected');
    botScore += 0.8;
  }
  
  if (navigator.plugins.length === 0) {
    indicators.push('No plugins detected');
    botScore += 0.2;
  }
  
  if (navigator.languages.length === 0) {
    indicators.push('No languages detected');
    botScore += 0.1;
  }
  
  // Check for automated behavior - use screen dimensions instead of outer dimensions
  if (window.screen && (window.screen.width === 0 || window.screen.height === 0)) {
    indicators.push('Zero window dimensions');
    botScore += 0.5;
  }
  
  // Positive indicators for human users - capture all 4 with values
  if (window.chrome && window.chrome.webstore) {
    // Normal Chrome browser - negative score (reduces bot score)
    botScore -= 0.1;
  }
  
  // Capture plugins count with specific value - handle mobile browsers
  const pluginsCount = navigator.plugins.length;
  indicators.push(`Plugins: ${pluginsCount}`);
  if (pluginsCount > 5) {
    // Human users typically have many plugins
    botScore -= 0.1;
  } else if (pluginsCount === 0) {
    // Mobile browsers often have 0 plugins - don't penalize heavily
    botScore += 0.1;
  } else {
    // Some plugins detected
    botScore -= 0.05;
  }
  
  // Capture languages count with specific value
  const languagesCount = navigator.languages.length;
  indicators.push(`Languages: ${languagesCount}`);
  if (languagesCount > 0) {
    // Human users have language preferences
    botScore -= 0.1;
  }
  
  // Capture screen resolution with specific value
  if (window.screen && window.screen.width > 0 && window.screen.height > 0) {
    // Normal screen dimensions
    indicators.push(`Screen: ${window.screen.width}x${window.screen.height}`);
    botScore -= 0.1;
  } else if (window.screen && (window.screen.width === 0 || window.screen.height === 0)) {
    // Mobile Chrome zero dimensions - use fallback detection
    indicators.push('Zero window dimensions');
    botScore += 0.2; // Increase bot score for suspicious zero dimensions
  } else if (window.screen && window.screen.width && window.screen.height) {
    // Fallback for valid but unusual dimensions
    indicators.push(`Screen: ${window.screen.width}x${window.screen.height}`);
    botScore -= 0.1;
  }
  
  // Capture user agent details - enhanced for mobile
  const browserMatch = userAgent.match(/(Chrome|Firefox|Safari|Edge)\/(\d+\.\d+)/);
  if (browserMatch) {
    const browserName = browserMatch[1];
    const browserVersion = browserMatch[2];
    
    // Detect mobile
    let deviceType = 'Desktop';
    if (userAgent.includes('Mobile') || userAgent.includes('Android') || userAgent.includes('iPhone')) {
      deviceType = 'Mobile';
    }
    
    indicators.push(`Browser: ${browserName} ${browserVersion} (${deviceType})`);
    botScore -= 0.1;
  } else {
    // Fallback for unknown browsers
    indicators.push(`Browser: Unknown`);
  }
  
  botScore = Math.max(0, Math.min(1, botScore));
  
  return {
    isBot: botScore > 0.5,
    botScore: botScore,
    indicators: indicators,
    userAgent: userAgent
  };
};
