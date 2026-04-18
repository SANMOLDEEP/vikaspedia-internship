// Browser and System Information Detection
export const getBrowserInfo = () => {
  const userAgent = navigator.userAgent;
  const info = {
    browser: 'Unknown',
    version: 'Unknown',
    os: 'Unknown',
    osVersion: 'Unknown',
    screenResolution: 'Unknown',
    language: 'Unknown',
    timezone: 'Unknown',
    deviceType: 'Desktop'
  };

  // Browser Detection
  const browserMatch = userAgent.match(/(Chrome|Firefox|Safari|Edge|Opera)\/(\d+\.\d+)/);
  if (browserMatch) {
    info.browser = browserMatch[1];
    info.version = browserMatch[2];
  }

  // Operating System Detection
  const osMatch = userAgent.match(/(Windows|Mac|Linux|Android|iOS)(?:\s+NT\s+(\d+\.\d+)?|\s+(\d+\.\d+)?|)?/);
  if (osMatch) {
    info.os = osMatch[1];
    if (osMatch[2]) {
      info.osVersion = osMatch[2];
    } else if (osMatch[3]) {
      info.osVersion = osMatch[3];
    }
  }

  // More specific OS detection
  if (userAgent.includes('Windows NT 10.0')) {
    // Detect if it's a laptop (common laptop indicators)
    const isLaptop = userAgent.includes('Laptop') || 
                     userAgent.includes('Notebook') ||
                     userAgent.includes('Mobile') === false && 
                     (userAgent.includes('Intel') || userAgent.includes('AMD')) &&
                     (window.screen.width <= 1920 && window.screen.height <= 1200) || 
                     (window.screen.width <= 1600 && window.screen.height <= 900) || 
                     (window.screen.width === 1536 && window.screen.height === 864);
    
    if (isLaptop) {
      info.os = 'Windows';
      info.osVersion = '';
      info.deviceType = 'Laptop';
    } else {
      info.os = 'Windows';
      info.osVersion = '';
      info.deviceType = 'Desktop';
    }
  } else if (userAgent.includes('Mac OS X')) {
    info.os = 'macOS';
  } else if (userAgent.includes('Android')) {
    info.os = 'Android';
    info.deviceType = 'Mobile';
  } else if (userAgent.includes('iPhone') || userAgent.includes('iPad')) {
    info.os = 'iOS';
    info.deviceType = 'Mobile';
  }

  // Screen Resolution
  if (window.screen) {
    info.screenResolution = `${window.screen.width}x${window.screen.height}`;
  }

  // Language Detection
  if (navigator.languages && navigator.languages.length > 0) {
    const primaryLang = navigator.languages[0];
    const langName = new Intl.DisplayNames([navigator.language], { type: 'language' }).of(primaryLang);
    info.language = langName || primaryLang;
  } else if (navigator.language) {
    const langName = new Intl.DisplayNames([navigator.language], { type: 'language' }).of(navigator.language);
    info.language = langName || navigator.language;
  }

  // Timezone Detection
  if (Intl.DateTimeFormat) {
    const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    info.timezone = timeZone;
  }

  // Device Type Refinement
  if (userAgent.includes('Mobile') || userAgent.includes('Android') || userAgent.includes('iPhone')) {
    info.deviceType = 'Mobile';
  } else if (userAgent.includes('Tablet') || userAgent.includes('iPad')) {
    info.deviceType = 'Tablet';
  }

  return info;
};
