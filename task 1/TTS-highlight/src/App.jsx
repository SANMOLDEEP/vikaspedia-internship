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
import Toast from './components/Toast';

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
  const [selectedLanguage, setSelectedLanguage] = useState('en-IN');
  const [speechRate, setSpeechRate] = useState(1);
  const [currentText, setCurrentText] = useState('');
  const [toastOpen, setToastOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState('');

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
        const langCode = selectedLanguage.split('.')[0];
        setToastMessage(`Voice not supported for ${langCode}.`);
        setToastOpen(true);
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



  const handleToastClose = () => {
    setToastOpen(false);
  };

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Container maxWidth="md" sx={{ py: 4 }}>
        <Paper elevation={3} sx={{ p: 3, mb: 3 }}>
          <Typography variant="h4" component="h1" gutterBottom align="center" color="primary">
            Task 1: Text Highlighting with Text-to-Speech
          </Typography>
          
          <Typography variant="body1" align="center" color="text.secondary" sx={{ mb: 3 }}>
            Synchronized Text-to-Speech with Real-time Highlighting
          </Typography>

          <Toast
            open={toastOpen}
            message={toastMessage}
            severity="warning"
            autoHideDuration={3000}
            onClose={handleToastClose}
          />

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
