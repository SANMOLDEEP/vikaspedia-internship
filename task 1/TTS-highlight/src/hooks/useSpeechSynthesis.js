import { useState, useEffect, useCallback, useRef } from 'react';

export const useSpeechSynthesis = () => {
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [isPaused, setIsPaused] = useState(false);
  const [isSupported, setIsSupported] = useState(true);
  const [currentWordIndex, setCurrentWordIndex] = useState(0);
  const [availableVoices, setAvailableVoices] = useState([]);
  const utteranceRef = useRef(null);
  const wordsRef = useRef([]);

  useEffect(() => {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      setIsSupported(true);
      
      // Load voices
      const loadVoices = () => {
        const voices = window.speechSynthesis.getVoices();
        setAvailableVoices(voices);
        console.log('Available voices:', voices.map(v => `${v.name} (${v.lang})`));
      };

      loadVoices();
      
      // Some browsers need this event
      window.speechSynthesis.onvoiceschanged = loadVoices;
    } else {
      setIsSupported(false);
    }

    return () => {
      if (window.speechSynthesis) {
        window.speechSynthesis.cancel();
      }
    };
  }, []);

  const prepareWords = useCallback((text) => {
    // Split text into words and calculate character positions
    const words = [];
    let currentPosition = 0;
    
    text.split(' ').forEach((word, index) => {
      const start = currentPosition;
      const end = currentPosition + word.length;
      
      words.push({
        word,
        index,
        start,
        end
      });
      
      currentPosition = end + 1; // +1 for space
    });
    
    console.log('Prepared words:', words.map(w => `${w.index}: "${w.word}" (${w.start}-${w.end})`));
    wordsRef.current = words;
    return words;
  }, []);

  const speak = useCallback((text, options = {}) => {
    if (!text || !isSupported) {
      console.log('Cannot speak: text empty or not supported');
      return;
    }

    // Wait for voices to load if not loaded yet
    if (availableVoices.length === 0) {
      console.log('Voices not loaded yet, waiting...');
      setTimeout(() => {
        const voices = window.speechSynthesis.getVoices();
        setAvailableVoices(voices);
        console.log('Reloaded voices:', voices.map(v => `${v.name} (${v.lang})`));
        // Retry speaking after voices are loaded
        speak(text, options);
      }, 1000);
      return;
    }

    if (window.speechSynthesis.speaking) {
      window.speechSynthesis.cancel();
    }

    // Directly speak the actual text without test speech
    console.log('Speaking actual text directly...');
    speakActualText(text, options);
  }, [isSupported, availableVoices, prepareWords, isSpeaking, isPaused]);

  const speakActualText = useCallback((text, options = {}) => {
    const words = prepareWords(text);
    const utterance = new SpeechSynthesisUtterance(text);
    
    utterance.lang = options.lang || 'en-US';
    utterance.rate = options.rate || 1;
    utterance.pitch = options.pitch || 1;
    utterance.volume = options.volume || 1;

    // Find appropriate voice for the selected language
    const findVoiceForLanguage = (langCode) => {
      console.log('Looking for voice for language:', langCode);
      console.log('Available voices count:', availableVoices.length);
      
      // Try to find exact match first
      let voice = availableVoices.find(v => v.lang === langCode);
      if (voice) {
        console.log('Found exact match:', voice.name, voice.lang);
        return voice;
      }
      
      // If not found, try matching language prefix
      const langPrefix = langCode.split('-')[0];
      console.log('Trying prefix match for:', langPrefix);
      voice = availableVoices.find(v => v.lang.startsWith(langPrefix));
      if (voice) {
        console.log('Found prefix match:', voice.name, voice.lang);
        return voice;
      }
      
      console.log('No voice found for:', langCode);
      return null;
    };

    // Set voice based on language with fallback
    if (options.lang && availableVoices.length > 0) {
      const voice = findVoiceForLanguage(options.lang);
      if (voice) {
        utterance.voice = voice;
        console.log('Selected voice:', voice.name, voice.lang, 'for language:', options.lang);
      } else {
        console.log('No voice found for language:', options.lang, '- using first available voice');
        // Force use first available voice as fallback
        if (availableVoices.length > 0) {
          utterance.voice = availableVoices[0];
          console.log('Fallback voice:', availableVoices[0].name, availableVoices[0].lang);
        }
        // Still set the language even if no specific voice
        utterance.lang = options.lang;
      }
    } else {
      console.log('No language specified or voices not loaded yet');
      // Try to speak anyway with default settings
      if (availableVoices.length > 0) {
        utterance.voice = availableVoices[0];
        console.log('Using default voice:', availableVoices[0].name);
      }
    }

    // Check if language is English for highlighting
    const isEnglish = options.lang && options.lang.startsWith('en');

    // Timer only for English highlighting
    let wordTimer = null;
    let currentWord = 0;

    const startWordTimer = () => {
      if (wordTimer) clearInterval(wordTimer);
      
      // Only start timer for English
      if (!isEnglish) {
        return;
      }
      
      const wordsPerSecond = 2.5 * (options.rate || 1);
      const intervalMs = 1000 / wordsPerSecond;
      
      wordTimer = setInterval(() => {
        if (currentWord < words.length && isSpeaking && !isPaused) {
          setCurrentWordIndex(currentWord);
          currentWord++;
        } else {
          // Clear timer if paused or stopped
          clearInterval(wordTimer);
          wordTimer = null;
        }
      }, intervalMs);
    };

    const resumeWordTimer = () => {
      if (!isEnglish || !isSpeaking || isPaused) return;
      
      const wordsPerSecond = 2.5 * (options.rate || 1);
      const intervalMs = 1000 / wordsPerSecond;
      
      wordTimer = setInterval(() => {
        if (currentWord < words.length && isSpeaking && !isPaused) {
          setCurrentWordIndex(currentWord);
          currentWord++;
        } else {
          clearInterval(wordTimer);
          wordTimer = null;
        }
      }, intervalMs);
    };

    // Only use boundary events for English
    if (isEnglish) {
      utterance.onboundary = (event) => {
        if (event.name === 'word') {
          const charIndex = event.charIndex;
          const wordIndex = words.findIndex(w => 
            charIndex >= w.start && charIndex <= w.end
          );
          
          if (wordIndex !== -1) {
            setCurrentWordIndex(wordIndex);
            currentWord = wordIndex + 1;
            currentWordRef.current = currentWord;
            
            // Stop timer if boundary events work
            if (wordTimer) {
              clearInterval(wordTimer);
              wordTimer = null;
            }
          }
        }
      };
    }

    utterance.onstart = () => {
      setIsSpeaking(true);
      setIsPaused(false);
      
      if (isEnglish) {
        setCurrentWordIndex(0);
        currentWord = 0;
        startWordTimer();
      } else {
        setCurrentWordIndex(-1);
        currentWord = 0;
      }
    };

    utterance.onend = () => {
      setIsSpeaking(false);
      setIsPaused(false);
      setCurrentWordIndex(0);
      if (wordTimer) clearInterval(wordTimer);
    };

    utterance.onerror = (event) => {
      console.error('Speech synthesis error:', event);
      setIsSpeaking(false);
      setIsPaused(false);
      setCurrentWordIndex(0);
      if (wordTimer) clearInterval(wordTimer);
    };

    utteranceRef.current = utterance;
    window.speechSynthesis.speak(utterance);
  }, [isSupported, availableVoices, prepareWords, isSpeaking, isPaused]);

  const pause = useCallback(() => {
    if (window.speechSynthesis && isSpeaking && !isPaused) {
      window.speechSynthesis.pause();
      setIsPaused(true);
      // Timer will clear itself in the next interval check
    }
  }, [isSpeaking, isPaused]);

  const resume = useCallback(() => {
    if (window.speechSynthesis && isSpeaking && isPaused) {
      window.speechSynthesis.resume();
      setIsPaused(false);
      // Restart timer after a small delay to ensure state is updated
      setTimeout(() => {
        if (utteranceRef.current && isSpeaking && !isPaused) {
          resumeWordTimer();
        }
      }, 50);
    }
  }, [isSpeaking, isPaused]);

  const stop = useCallback(() => {
    if (window.speechSynthesis) {
      window.speechSynthesis.cancel();
      setIsSpeaking(false);
      setIsPaused(false);
      setCurrentWordIndex(0);
    }
  }, []);

  return {
    isSpeaking,
    isPaused,
    isSupported,
    currentWordIndex,
    availableVoices,
    speak,
    pause,
    resume,
    stop,
    prepareWords
  };
};
