import React, { useState } from 'react';
import { Box, Typography, TextField, Button } from '@mui/material';
import { Edit as EditIcon } from '@mui/icons-material';

const TextHighlighter = ({ 
  text, 
  currentWordIndex, 
  isSpeaking,
  onTextChange,
  fontSize = '1.1rem',
  lineHeight = 1.6 
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [editedText, setEditedText] = useState(text);

  if (!text) return null;

  const handleEdit = () => {
    setIsEditing(true);
    setEditedText(text);
  };

  const handleSave = () => {
    setIsEditing(false);
    if (onTextChange && editedText.trim()) {
      onTextChange(editedText.trim());
    }
  };

  const handleCancel = () => {
    setIsEditing(false);
    setEditedText(text);
  };

  const handleTextChange = (event) => {
    setEditedText(event.target.value);
  };

  // Use current text (not edited) for highlighting during speech
  const displayText = isSpeaking ? text : (isEditing ? editedText : text);
  const words = displayText.split(' ');
  
  return (
    <Box sx={{ my: 3, p: 2, bgcolor: 'background.paper', borderRadius: 2 }}>
      {/* Edit/Save/Cancel Controls */}
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 1, gap: 1 }}>
        {!isSpeaking && (
          <>
            {!isEditing ? (
              <Button 
                onClick={handleEdit} 
                size="small" 
                variant="outlined"
                startIcon={<EditIcon fontSize="small" />}
              >
                Edit
              </Button>
            ) : (
              <>
                <Button 
                  onClick={handleSave} 
                  size="small" 
                  variant="contained"
                  color="success"
                >
                  Save
                </Button>
                <Button 
                  onClick={handleCancel} 
                  size="small" 
                  variant="outlined"
                  color="error"
                >
                  Cancel
                </Button>
              </>
            )}
          </>
        )}
      </Box>

      {/* Text Display/Edit Area */}
      {isEditing ? (
        <TextField
          multiline
          fullWidth
          variant="outlined"
          value={editedText}
          onChange={handleTextChange}
          placeholder="Enter your text here..."
          sx={{
            '& .MuiOutlinedInput-root': {
              fontSize,
              lineHeight,
              fontFamily: 'inherit'
            }
          }}
        />
      ) : (
        <Typography
          variant="body1"
          component="div"
          sx={{
            fontSize,
            lineHeight,
            textAlign: 'justify',
            fontFamily: 'inherit',
            minHeight: '3em',
            cursor: isSpeaking ? 'default' : 'text'
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
      )}
    </Box>
  );
};

export default TextHighlighter;
