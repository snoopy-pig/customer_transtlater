import { useEffect, useRef, useState } from 'react';

export default function useSpeechToText({ lang = 'ko-KR', onTranscript, onSentenceEnd }) {
  const [isListening, setIsListening] = useState(false);
  const [error, setError] = useState(null);
  
  const recognitionRef = useRef(null);
  const shouldBeListeningRef = useRef(false);

  // Wrap callbacks in refs to avoid stale closure issues
  const onTranscriptRef = useRef(onTranscript);
  const onSentenceEndRef = useRef(onSentenceEnd);

  // Update refs on every render
  useEffect(() => {
    onTranscriptRef.current = onTranscript;
    onSentenceEndRef.current = onSentenceEnd;
  });



  // Initialize SpeechRecognition
  useEffect(() => {
    if (typeof window === 'undefined') return;

    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
      setError('Speech Recognition API not supported in this browser. Please use Chrome, Edge, or Safari.');
      return;
    }

    const recognition = new SpeechRecognition();
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.lang = lang;

    recognition.onstart = () => {
      setIsListening(true);
      setError(null);
    };

    recognition.onerror = (event) => {
      // Ignorable non-fatal errors
      if (event.error === 'no-speech' || event.error === 'aborted') {
        console.warn('Speech recognition status:', event.error);
        return;
      }
      
      console.error('Speech recognition error:', event.error);
      if (event.error === 'not-allowed') {
        setError('Microphone permission denied.');
        shouldBeListeningRef.current = false;
        setIsListening(false);
      } else {
        setError(`Recognition error: ${event.error}`);
      }
    };

    recognition.onend = () => {
      // Prevent setting isListening to false during auto-restarts to avoid UI flickering
      if (!shouldBeListeningRef.current) {
        setIsListening(false);
      }
      // Self-healing: If we should be listening, restart the engine!
      if (shouldBeListeningRef.current) {
        try {
          recognition.start();
        } catch (e) {
          console.error('Failed to restart speech recognition:', e);
        }
      }
    };

    recognition.onresult = (event) => {
      let interimTranscript = '';
      let finalTranscript = '';

      for (let i = event.resultIndex; i < event.results.length; ++i) {
        const result = event.results[i];
        if (result.isFinal) {
          finalTranscript += result[0].transcript;
        } else {
          interimTranscript += result[0].transcript;
        }
      }

      if (interimTranscript.trim() || finalTranscript.trim()) {
        if (onTranscriptRef.current) {
          onTranscriptRef.current({
            interimText: interimTranscript,
            finalText: finalTranscript,
            isFinal: finalTranscript.length > 0
          });
        }
      }

      if (finalTranscript.trim()) {
        if (onSentenceEndRef.current) {
          onSentenceEndRef.current(finalTranscript.trim());
        }
      }
    };

    recognitionRef.current = recognition;

    if (shouldBeListeningRef.current) {
      try {
        recognition.start();
      } catch (e) {
        console.warn('Failed to auto-restart recognition on language change:', e);
      }
    }

    return () => {
      if (recognitionRef.current) {
        recognitionRef.current.abort();
      }
    };
  }, [lang]); // Recreate when language changes to apply recognition.lang

  const startListening = () => {
    if (!recognitionRef.current) return;
    shouldBeListeningRef.current = true;
    setIsListening(true);
    try {
      recognitionRef.current.start();
    } catch (e) {
      console.warn('SpeechRecognition is already running or failed to start:', e);
    }
  };

  const stopListening = () => {
    shouldBeListeningRef.current = false;
    setIsListening(false);
    if (!recognitionRef.current) return;
    try {
      recognitionRef.current.stop();
    } catch (e) {
      console.warn('Failed to stop speech recognition:', e);
    }
  };

  return {
    isListening,
    error,
    startListening,
    stopListening
  };
}
