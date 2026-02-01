import React from 'react';
import { Alert, AlertTitle, Box } from '@mui/material';
import { Warning as WarningIcon } from '@mui/icons-material';

const FallbackAlert = ({ 
  isSupported, 
  selectedLanguage, 
  availableVoices = []
}) => {
  if (!isSupported) {
    return (
      <Alert 
        severity="error" 
        icon={<WarningIcon />}
        sx={{ mb: 2 }}
      >
        <AlertTitle>Text-to-Speech Not Supported</AlertTitle>
        Your browser does not support the Web Speech API. Please try using a modern browser like Chrome, Firefox, Edge, or Safari.
      </Alert>
    );
  }

  // Only show simple loading message
  if (availableVoices.length === 0) {
    return (
      <Alert severity="info" sx={{ mb: 2 }}>
        <AlertTitle>Loading Voices</AlertTitle>
        Speech synthesis voices are loading. Please wait a moment.
      </Alert>
    );
  }

  return null; // No alerts by default
};

export default FallbackAlert;
