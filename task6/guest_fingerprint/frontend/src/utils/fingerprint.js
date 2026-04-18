import FingerprintJS from '@fingerprintjs/fingerprintjs';

// Use same configuration as advancedFingerprint.js for consistency
const fpPromise = FingerprintJS.load({
  apiKey: 'your-api-key', // Optional: Get from FingerprintJS
  region: 'eu' // Optional: Choose region
});

export const getFingerprint = async () => {
  const fp = await fpPromise;
  const result = await fp.get();
  return result.visitorId;
};

export const getBrowserProfile = async () => {
  const fp = await fpPromise;
  const result = await fp.get();
  
  // Extract browser information
  const browserInfo = {
    userAgent: navigator.userAgent,
    language: navigator.language,
    platform: navigator.platform,
    vendor: navigator.vendor,
    fingerprint: result.visitorId,
    components: result.components
  };
  
  // Generate a user-friendly name from browser data
  let userName = "Browser User";
  let browserType = "browser";
  let platformType = "unknown";
  
  // Detect browser type
  if (navigator.userAgent.includes("Chrome")) {
    userName = "Chrome User";
    browserType = "chrome";
  } else if (navigator.userAgent.includes("Firefox")) {
    userName = "Firefox User";
    browserType = "firefox";
  } else if (navigator.userAgent.includes("Safari")) {
    userName = "Safari User";
    browserType = "safari";
  } else if (navigator.userAgent.includes("Edge")) {
    userName = "Edge User";
    browserType = "edge";
  }
  
  // Detect platform type - check userAgent first for mobile devices
  if (navigator.userAgent.includes("Android")) {
    userName += " (Android)";
    platformType = "android";
  } else if (navigator.userAgent.includes("iPhone") || navigator.userAgent.includes("iPad") || navigator.userAgent.includes("iPod")) {
    userName += " (iOS)";
    platformType = "ios";
  } else if (navigator.platform.includes("Win")) {
    userName += " (Windows)";
    platformType = "windows";
  } else if (navigator.platform.includes("Mac")) {
    userName += " (Mac)";
    platformType = "mac";
  } else if (navigator.platform.includes("Linux")) {
    userName += " (Linux)";
    platformType = "linux";
  }
  
  // Generate dynamic email based on browser and platform
  const dynamicEmail = `${browserType}.${platformType}@fingerprint.local`;
  
  return {
    ...browserInfo,
    userName: userName,
    browserType: browserType,
    platformType: platformType,
    dynamicEmail: dynamicEmail
  };
};
