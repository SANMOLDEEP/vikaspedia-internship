import React, { useState, useEffect } from 'react';
import {
  Container,
  Typography,
  Box,
  Slider,
  Paper,
  ThemeProvider,
  createTheme,
  CssBaseline
} from '@mui/material';
import {
  Speed as SpeedIcon,
  VolumeUp as VolumeIcon
} from '@mui/icons-material';

import { useSpeechSynthesis } from './hooks/useSpeechSynthesis';
import { getSampleText } from './data/sampletxt';
import Controls from './components/Controls';
import LanguageSelector from './components/LanguageSelector';
import TextHighlighter from './components/TextHighlighter';
import FallbackAlert from './components/FallbackAlert';

// Import languages for alert messages
const languages = [
  { code: 'en-US', name: 'English (US)', flag: '🇺🇸' },
  { code: 'en-IN', name: 'English (India)', flag: '🇮🇳' },
  { code: 'hi-IN', name: 'हिन्दी (Hindi)', flag: '🇮🇳' },
  { code: 'gu-IN', name: 'ગુજરાતી (Gujarati)', flag: '🇮🇳' },
  { code: 'mr-IN', name: 'मराठी (Marathi)', flag: '🇮🇳' },
  { code: 'ta-IN', name: 'தமிழ் (Tamil)', flag: '🇮🇳' },
  { code: 'te-IN', name: 'తెలుగు (Telugu)', flag: '🇮🇳' }
];

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
  },
  typography: {
    h4: {
      fontWeight: 600,
    },
    h6: {
      fontWeight: 500,
    }
  },
});

function App() {
  const [selectedLanguage, setSelectedLanguage] = useState('en-US');
  const [speechRate, setSpeechRate] = useState(1);
  const [currentText, setCurrentText] = useState('');

  const {
    isSpeaking,
    isPaused,
    isSupported,
    currentWordIndex,
    availableVoices,
    speak,
    pause,
    resume,
    stop
  } = useSpeechSynthesis();

  useEffect(() => {
    const sampleText = getSampleText(selectedLanguage);
    setCurrentText(sampleText);
  }, [selectedLanguage]);

  const handlePlay = () => {
    if (currentText && currentText.trim()) {
      const hasVoiceForLanguage = availableVoices.some(voice => 
        voice.lang === selectedLanguage || voice.lang.startsWith(selectedLanguage.split('-')[0])
      );
      
      if (!hasVoiceForLanguage) {
        const langName = languages.find(lang => lang.code === selectedLanguage)?.name || selectedLanguage;
        alert(`${langName} is not supported by this browser.`);
        return;
      }
      
      speak(currentText, {
        lang: selectedLanguage,
        rate: speechRate
      });
    }
  };

  const handlePause = () => {
    pause();
  };

  const handleResume = () => {
    resume();
  };

  const handleStop = () => {
    stop();
  };

  const handleTextChange = (newText) => {
    setCurrentText(newText);
  };

  const handleLanguageChange = (newLanguage) => {
    if (isSpeaking) {
      stop();
    }
    setSelectedLanguage(newLanguage);
  };

  const handleRateChange = (event, newValue) => {
    setSpeechRate(newValue);
  };

  const sampleData = getSampleText(selectedLanguage);



  
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Container maxWidth="md" sx={{ py: 4 }}>
        <Paper elevation={3} sx={{ p: 3, mb: 3 }}>
          <Typography variant="h4" component="h1" gutterBottom align="center" color="primary">
            Task: Text Highlighting with Text-to-Speech
          </Typography>
          
          <Typography variant="body1" align="center" color="text.secondary" sx={{ mb: 3 }}>
            Synchronized Text-to-Speech with Real-time Highlighting
          </Typography>

          <FallbackAlert
            isSupported={isSupported}
            selectedLanguage={selectedLanguage}
            availableVoices={availableVoices}
          />

          {isSupported && (
            <Box>
              {/* Language Selection */}
              <LanguageSelector
                selectedLanguage={selectedLanguage}
                onLanguageChange={handleLanguageChange}
                availableVoices={availableVoices}
                disabled={isSpeaking}
              />


              {/* Text Title */}
              <Typography variant="h6" component="h2" gutterBottom sx={{ mt: 3, mb: 2 }}>
                Content
              </Typography>

              {/* Text Highlighter */}
              <TextHighlighter
                text={currentText}
                currentWordIndex={currentWordIndex}
                isSpeaking={isSpeaking}
                onTextChange={handleTextChange}
              />

              {/* Speech Rate Control */}
              <Box sx={{ px: 2, py: 2, bgcolor: 'grey.50', borderRadius: 2, mb: 2 }}>
                <Typography variant="body2" gutterBottom>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <SpeedIcon fontSize="small" />
                    Speech Rate: {speechRate.toFixed(1)}x
                  </Box>
                </Typography>
                <Slider
                  value={speechRate}
                  onChange={handleRateChange}
                  min={0.5}
                  max={2.0}
                  step={0.1}
                  marks={[
                    { value: 0.5, label: '0.5x' },
                    { value: 1.0, label: '1.0x' },
                    { value: 1.5, label: '1.5x' },
                    { value: 2.0, label: '2.0x' }
                  ]}
                  valueLabelDisplay="auto"
                  disabled={isSpeaking || isPaused}
                />
              </Box>

              {/* Controls */}
              <Controls
                isSpeaking={isSpeaking}
                isPaused={isPaused}
                onPlay={handlePlay}
                onPause={handlePause}
                onResume={handleResume}
                onStop={handleStop}
                disabled={!currentText || availableVoices.length === 0}
              />
            </Box>
          )}
        </Paper>

        {/* Footer */}
        <Box sx={{ textAlign: 'center', mt: 3 }}>
          <Typography variant="body2" color="text.secondary">
            Supports Indian languages • Works on desktop & mobile • Browser-based TTS
          </Typography>
        </Box>
      </Container>
    </ThemeProvider>
  );
}

export default App;
