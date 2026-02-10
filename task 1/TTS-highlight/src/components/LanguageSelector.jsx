import React from 'react';
import { FormControl, InputLabel, Select, MenuItem, Box } from '@mui/material';
import { Language as LanguageIcon } from '@mui/icons-material';

const languages = [
  { code: 'en-US', name: 'English (US)', flag: '🇺🇸' },
  { code: 'en-IN', name: 'English (India)', flag: '🇮🇳' },
  { code: 'hi-IN', name: 'हिन्दी (Hindi)', flag: '🇮🇳' },
  { code: 'gu-IN', name: 'ગુજરાતી (Gujarati)', flag: '🇮🇳' },
  { code: 'mr-IN', name: 'मराठी (Marathi)', flag: '🇮🇳' },
  { code: 'ta-IN', name: 'தமிழ் (Tamil)', flag: '🇮🇳' },
  { code: 'te-IN', name: 'తెలుగు (Telugu)', flag: '🇮🇳' }
];

const LanguageSelector = ({ 
  selectedLanguage, 
  onLanguageChange, 
  availableVoices = [],
  disabled = false 
}) => {
  const isLanguageSupported = (langCode) => {
    // Check if browser has voices available for this language
    return availableVoices.some(voice => 
      voice.lang === langCode || voice.lang.startsWith(langCode.split('-')[0])
    );
  };

  return (
    <Box sx={{ minWidth: 200, my: 2 }}>
      <FormControl fullWidth size="small" sx={{
        '& .MuiOutlinedInput-root': {
          '& fieldset': {
            borderColor: 'rgba(0, 0, 0, 0.23)',
          },
          '&:hover fieldset': {
            borderColor: 'rgba(0, 0, 0, 0.87)',
          },
          '&.Mui-focused fieldset': {
            borderColor: '#1976d2',
          },
        }
      }}>
        <InputLabel id="language-select-label">
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <LanguageIcon fontSize="small" />
            Language
          </Box>
        </InputLabel>
        <Select
          labelId="language-select-label"
          id="language-select"
          value={selectedLanguage}
          label="Language"
          onChange={(e) => onLanguageChange(e.target.value)}
          disabled={disabled}
          sx={{
            '& .MuiSelect-select': {
              display: 'flex',
              alignItems: 'center',
              gap: 1,
            }
          }}
        >
          {languages.map((lang) => {
            return (
              <MenuItem 
                key={lang.code} 
                value={lang.code}
                sx={{
                  minHeight: 'auto',
                  py: 1
                }}
              >
                <Box sx={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  gap: 1, 
                  width: '100%',
                  overflow: 'hidden'
                }}>
                  <span style={{ 
                    fontSize: '1.1em', 
                    lineHeight: 1, 
                    flexShrink: 0 
                  }}>{lang.flag}</span>
                  <span style={{ 
                    fontSize: '0.9em', 
                    flexGrow: 1,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap'
                  }}>{lang.name}</span>
                </Box>
              </MenuItem>
            );
          })}
        </Select>
      </FormControl>
    </Box>
  );
};

export default LanguageSelector;
