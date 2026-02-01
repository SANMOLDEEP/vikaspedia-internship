import React, { useEffect } from 'react';
import {
  Alert,
  Snackbar,
  Box
} from '@mui/material';
import {
  Error as ErrorIcon
} from '@mui/icons-material';

const Toast = ({ 
  open, 
  message, 
  severity = 'error', 
  autoHideDuration = 3000,
  onClose 
}) => {
  useEffect(() => {
    if (open && autoHideDuration > 0) {
      const timer = setTimeout(() => {
        onClose();
      }, autoHideDuration);

      return () => clearTimeout(timer);
    }
  }, [open, autoHideDuration, onClose]);

  return (
    <Snackbar
      open={open}
      onClose={onClose}
      anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      sx={{
        mt: 2
      }}
    >
      <Alert 
        onClose={onClose} 
        severity={severity}
        iconMapping={{
          error: <ErrorIcon fontSize="inherit" />,
        }}
        sx={{
          minWidth: '300px',
          '& .MuiAlert-message': {
            fontSize: '0.9rem'
          }
        }}
      >
        {message}
      </Alert>
    </Snackbar>
  );
};

export default Toast;
