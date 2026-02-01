import React from 'react';
import { Box, Button, ButtonGroup } from '@mui/material';
import {
  PlayArrow as PlayIcon,
  Pause as PauseIcon,
  Stop as StopIcon,
  Refresh as ResumeIcon
} from '@mui/icons-material';

const Controls = ({ 
  isSpeaking, 
  isPaused, 
  onPlay, 
  onPause, 
  onResume, 
  onStop,
  disabled = false 
}) => {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', my: 2 }}>
      <ButtonGroup variant="contained" aria-label="speech controls">
        {!isSpeaking ? (
          <Button
            startIcon={<PlayIcon />}
            onClick={onPlay}
            disabled={disabled}
            size="large"
          >
            Play
          </Button>
        ) : (
          <>
            {!isPaused ? (
              <Button
                startIcon={<PauseIcon />}
                onClick={onPause}
                size="large"
              >
                Pause
              </Button>
            ) : (
              <Button
                startIcon={<ResumeIcon />}
                onClick={onResume}
                size="large"
              >
                Resume
              </Button>
            )}
            <Button
              startIcon={<StopIcon />}
              onClick={onStop}
              size="large"
              color="error"
            >
              Stop
            </Button>
          </>
        )}
      </ButtonGroup>
    </Box>
  );
};

export default Controls;
