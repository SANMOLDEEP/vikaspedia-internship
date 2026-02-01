import React from 'react';
import { Box, Typography } from '@mui/material';

const TextHighlighter = ({ 
  text, 
  currentWordIndex, 
  isSpeaking,
  fontSize = '1.1rem',
  lineHeight = 1.6 
}) => {
  if (!text) return null;

  const words = text.split(' ');
  
  return (
    <Box sx={{ my: 3, p: 2, bgcolor: 'background.paper', borderRadius: 2 }}>
      <Typography
        variant="body1"
        component="div"
        sx={{
          fontSize,
          lineHeight,
          textAlign: 'justify',
          fontFamily: 'inherit'
        }}
      >
        {words.map((word, index) => {
          const isHighlighted = index === currentWordIndex && isSpeaking && currentWordIndex >= 0;
          
          return (
            <span
              key={index}
              style={{
                backgroundColor: isHighlighted ? '#ffeb3b' : 'transparent',
                padding: isHighlighted ? '2px 4px' : '0',
                borderRadius: isHighlighted ? '4px' : '0',
                transition: 'background-color 0.2s ease-in-out',
                display: 'inline-block',
                marginRight: '0.3em'
              }}
            >
              {word}
            </span>
          );
        })}
      </Typography>
    </Box>
  );
};

export default TextHighlighter;
