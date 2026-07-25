'use client';

import React, { useState, useEffect, useRef, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import styles from './Room.module.css';
import Sidebar, { LANGUAGES } from '../../components/Sidebar';
import TranslationArea from '../../components/TranslationArea';
import HistoryArea from '../../components/HistoryArea';
import useSpeechToText from '../../hooks/useSpeechToText';
import { 
  subscribeSession, 
  subscribeHistory, 
  updateSession, 
  addHistoryEntry, 
  clearHistory 
} from '../../lib/sync';
import { exportSessionToHtml } from '../../lib/export';

function RoomContent() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const roomId = searchParams.get('room');
  const role = searchParams.get('role'); // 'seller' or 'buyer'

  const [sessionData, setSessionData] = useState(null);
  const [history, setHistory] = useState([]);
  const [completedSessions, setCompletedSessions] = useState([]);
  const [activeCompletedSessionId, setActiveCompletedSessionId] = useState(null);
  const [micError, setMicError] = useState(null);

  // Keep track of the current history in a ref so speech end callback can access it
  const historyRef = useRef([]);
  useEffect(() => {
    historyRef.current = history;
  }, [history]);

  // Load completed sessions list from localStorage
  useEffect(() => {
    if (!roomId) return;
    const saved = localStorage.getItem(`completed_sessions_${roomId}`);
    if (saved) {
      setCompletedSessions(JSON.parse(saved));
    }
  }, [roomId]);

  // Redirect to lobby if parameters are missing
  useEffect(() => {
    if (!roomId || !role || (role !== 'seller' && role !== 'buyer')) {
      router.push('/');
    }
  }, [roomId, role, router]);

  // Subscribe to shared session state and active dialogue history
  useEffect(() => {
    if (!roomId) return;

    const unsubscribeSession = subscribeSession(roomId, (data) => {
      setSessionData(data);
    });

    const unsubscribeHistory = subscribeHistory(roomId, (data) => {
      setHistory(data);
    });

    return () => {
      unsubscribeSession();
      unsubscribeHistory();
    };
  }, [roomId]);

  // Get current active language depending on client role
  const getActiveLanguage = () => {
    if (!sessionData?.languages) return 'ko-KR';
    return role === 'seller' ? sessionData.languages.seller : sessionData.languages.buyer;
  };

  const getTargetLanguage = () => {
    if (!sessionData?.languages) return 'en-US';
    return role === 'seller' ? sessionData.languages.buyer : sessionData.languages.seller;
  };

  // Helper function to translate text via our serverless API route (Streaming)
  const translateText = async (text, sourceLang, targetLang, onChunk) => {
    try {
      const response = await fetch('/api/translate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          text,
          sourceLang,
          targetLang,
          glossary: sessionData?.glossary || []
        })
      });
      
      if (!response.ok) {
        console.warn('Translation API returned non-ok response');
        return `[번역 실패 - 원문]: ${text}`;
      }
      
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let accumulatedText = '';
      
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        const chunk = decoder.decode(value, { stream: true });
        accumulatedText += chunk;
        onChunk(accumulatedText);
      }
      
      return accumulatedText;
    } catch (error) {
      console.warn('Translation fetch network error:', error);
      return `[번역 실패 - 원문]: ${text}`;
    }
  };

  // Speech Recognition hook bindings
  const { isListening, startListening, stopListening } = useSpeechToText({
    lang: getActiveLanguage(),
    
    // Fired in real-time as the user speaks (interim updates)
    onTranscript: async (transcript) => {
      if (!roomId) return;
      
      const payload = {
        originalText: transcript.interimText || transcript.finalText,
        translatedText: '',
        isFinal: false,
        timestamp: Date.now()
      };

      // Write to role-specific live transcript channel
      if (role === 'seller') {
        await updateSession(roomId, { liveTranscriptSeller: payload });
      } else {
        await updateSession(roomId, { liveTranscriptBuyer: payload });
      }
    },

    // Fired when a sentence is finalized (speaker paused)
    onSentenceEnd: async (finalText) => {
      if (!roomId || !finalText.trim()) return;

      // Software Noise Gate: Ignore short noisy filler inputs (like coughs, keyboard taps, breathing, or brief filler sounds)
      if (finalText.trim().length < 3) {
        console.log("Ignoring short noise transcript:", finalText);
        return;
      }

      const sourceLang = getActiveLanguage();
      const targetLang = getTargetLanguage();

      const payloadTranslating = {
        originalText: finalText,
        translatedText: '',
        isFinal: false,
        timestamp: Date.now()
      };

      // Show translating state in DB
      if (role === 'seller') {
        await updateSession(roomId, { liveTranscriptSeller: payloadTranslating });
      } else {
        await updateSession(roomId, { liveTranscriptBuyer: payloadTranslating });
      }

      let lastUpdate = 0;

      // Call Gemini translation with chunk callback (Streaming)
      const translatedText = await translateText(
        finalText, 
        sourceLang, 
        targetLang, 
        async (accumulated) => {
          // Throttle DB updates to avoid overloading database
          const now = Date.now();
          if (now - lastUpdate > 300) {
            const payloadChunk = {
              originalText: finalText,
              translatedText: accumulated,
              isFinal: false,
              timestamp: Date.now()
            };

            if (role === 'seller') {
              await updateSession(roomId, { liveTranscriptSeller: payloadChunk });
            } else {
              await updateSession(roomId, { liveTranscriptBuyer: payloadChunk });
            }
            lastUpdate = now;
          }
        }
      );

      // Clean up any potential markdown quotes from translatedText
      let cleanedText = translatedText || '';
      if (cleanedText.startsWith('"') && cleanedText.endsWith('"')) {
        cleanedText = cleanedText.slice(1, -1);
      }

      // Append finalized dialogue entry to shared history IMMEDIATELY
      await addHistoryEntry(roomId, {
        id: `msg-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
        speaker: role,
        originalText: finalText,
        translatedText: cleanedText,
        timestamp: Date.now()
      });

      // Clear the live transcript block instantly
      if (role === 'seller') {
        await updateSession(roomId, { liveTranscriptSeller: null });
      } else {
        await updateSession(roomId, { liveTranscriptBuyer: null });
      }
    }
  });

  // Microphone Collision Management Effect
  useEffect(() => {
    if (!sessionData) return;
    const { status, activeMic } = sessionData;

    // 1. If meeting ends, stop listening
    if (status === 'ended' && isListening) {
      stopListening();
    }

    // 2. If another role claims the microphone, stop our mic (alternating lock)
    if (activeMic && activeMic !== role && isListening) {
      console.log(`Microphone grabbed by ${activeMic}. Muting our microphone.`);
      stopListening();
    }
  }, [sessionData?.status, sessionData?.activeMic, isListening, stopListening, role]);

  // Alternating Toggle Mic: sets activeMic in DB
  const handleToggleMic = async () => {
    if (!roomId) return;
    if (isListening) {
      stopListening();
      await updateSession(roomId, { activeMic: null });
    } else {
      await updateSession(roomId, { activeMic: role });
      startListening();
    }
  };

  // End Session (Seller only)
  const handleEndSession = async () => {
    if (role !== 'seller') return;
    if (!window.confirm('상담 세션을 종료하시겠습니까? 현재 기록이 완료된 세션으로 저장되며 보고서 파일이 다운로드됩니다.')) return;

    stopListening();

    // Get current timer info
    const startTime = sessionData?.timerStartTime || Date.now();
    const duration = Math.floor((Date.now() - startTime) / 1000);

    // Create completed session record
    const buyerName = sessionData?.buyerInfo?.name || '바이어';
    const buyerCompany = sessionData?.buyerInfo?.company || '회사';
    
    const newCompletedSession = {
      id: Date.now(),
      title: `${buyerName} (${buyerCompany})`,
      date: new Date().toLocaleDateString(),
      duration: duration >= 0 ? duration : 0,
      history: [...history], // Copy current active dialogue history
      buyerInfo: { ...sessionData?.buyerInfo },
      keywords: sessionData?.sellerKeywords || '',
      languages: { ...sessionData?.languages }
    };

    // Auto-download HTML report BEFORE resetting database history!
    try {
      exportSessionToHtml({ 
        roomId, 
        sessionData, 
        history 
      });
    } catch (err) {
      console.error("Auto HTML export failed on session end:", err);
    }

    // Update completed sessions list in localStorage
    const updatedSessions = [newCompletedSession, ...completedSessions];
    setSessionData(prev => prev ? { ...prev, status: 'ended' } : null); // Optimistic UI update
    setCompletedSessions(updatedSessions);
    localStorage.setItem(`completed_sessions_${roomId}`, JSON.stringify(updatedSessions));

    // Reset room in database
    await clearHistory(roomId);
    await updateSession(roomId, {
      status: 'ended',
      timerDuration: duration,
      liveTranscriptSeller: null,
      liveTranscriptBuyer: null,
      activeMic: null
    });

    alert('상담이 종료되었으며 미팅 리포트 파일이 다운로드되었습니다.');
  };

  // View historical completed session logs
  const handleViewCompletedSession = (session) => {
    setActiveCompletedSessionId(session.id);
  };

  const handleBackToLive = () => {
    setActiveCompletedSessionId(null);
  };

  // Determine what dialogue history list to pass to HistoryArea
  const getDisplayHistory = () => {
    if (activeCompletedSessionId) {
      const session = completedSessions.find(s => s.id === activeCompletedSessionId);
      return session ? session.history : [];
    }
    return history;
  };

  // Get session data depending on if viewing history or active live
  const getDisplaySessionData = () => {
    if (activeCompletedSessionId) {
      const session = completedSessions.find(s => s.id === activeCompletedSessionId);
      if (session) {
        return {
          buyerInfo: session.buyerInfo,
          sellerKeywords: session.keywords,
          languages: session.languages,
          status: 'ended'
        };
      }
    }
    return sessionData;
  };

  // Handler to configure and start a new meeting in the same room (Seller only, called from Sidebar)
  const handleStartNewMeetingFromSidebar = async (newBuyerInfo) => {
    // Clean historical active session data
    await clearHistory(roomId);
    
    // Set up new meeting parameters in database
    await updateSession(roomId, {
      buyerInfo: {
        name: newBuyerInfo.name,
        company: newBuyerInfo.company,
        extra: newBuyerInfo.extra || ''
      },
      sellerKeywords: '',
      status: 'active',
      timerStartTime: Date.now(),
      timerDuration: 0,
      liveTranscriptSeller: null,
      liveTranscriptBuyer: null,
      activeMic: null
    });
  };

  if (!sessionData) {
    return (
      <div style={{ display: 'flex', height: '100vh', justifyContent: 'center', alignItems: 'center', backgroundColor: 'var(--bg-dark)' }}>
        <p style={{ color: 'var(--color-text-secondary)', fontSize: '1rem' }}>회의실 데이터를 불러오고 있습니다...</p>
      </div>
    );
  }

  const isMeetingActive = sessionData.status === 'active';

  // Buyer Standby Screen (Buyer has no Sidebar layout when not active)
  if (!isMeetingActive && role === 'buyer') {
    return (
      <div style={{ height: '100vh', width: '100vw', display: 'flex', justifyContent: 'center', alignItems: 'center', backgroundColor: 'var(--bg-dark)', padding: '1rem' }}>
        <div className="glass-panel" style={{ padding: '3rem', borderRadius: '24px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '2rem', border: '1px solid var(--border-color)', boxShadow: '0 8px 32px 0 rgba(0, 0, 0, 0.37)', textAlign: 'center', maxWidth: '450px' }}>
          <div style={{ fontSize: '3.5rem', animation: 'bounce 2s infinite' }}>🤝</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <h2 style={{ color: 'white', margin: 0, fontSize: '1.4rem', fontFamily: 'system-ui' }}>대기 중</h2>
            <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.9rem', margin: 0, lineHeight: '1.5' }}>
              셀러가 회의 준비(바이어 정보 및 번역 언어 설정)를 진행하고 있습니다.<br/>
              잠시만 대기해 주시면 셀러가 회의를 시작하는 즉시 화면이 전환됩니다.
            </p>
          </div>
          
          <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'center' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', backgroundColor: 'var(--color-accent)', display: 'inline-block' }}></span>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', backgroundColor: 'var(--color-accent)', display: 'inline-block' }}></span>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', backgroundColor: 'var(--color-accent)', display: 'inline-block' }}></span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.roomContainer}>
      {/* 1. Left Sidebar panel */}
      <div className={styles.leftPanel}>
        <Sidebar
          roomId={roomId}
          sessionData={getDisplaySessionData()}
          role={role}
          onEndSession={handleEndSession}
          completedSessions={completedSessions}
          onViewCompletedSession={handleViewCompletedSession}
          activeCompletedSessionId={activeCompletedSessionId}
          onBackToLive={handleBackToLive}
          onStartNewMeeting={handleStartNewMeetingFromSidebar}
        />
      </div>

      {/* 2. Middle Live Workspace */}
      <div className={styles.middlePanel}>
        {isMeetingActive ? (
          <TranslationArea
            roomId={roomId}
            sessionData={sessionData} // Center always displays live state
            role={role}
            isListening={isListening}
            onToggleMic={handleToggleMic}
            micError={micError}
          />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', height: '100%', justifyContent: 'center', alignItems: 'center', padding: '2rem', textAlign: 'center', gap: '1.5rem' }}>
            <div style={{ fontSize: '4.5rem', animation: 'pulse 2s infinite' }}>💼</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', maxWidth: '500px' }}>
              <h2 style={{ color: 'var(--color-accent)', margin: 0, fontSize: '1.6rem', fontFamily: 'var(--font-display)', fontWeight: 700 }}>
                미팅 시작 대기 중
              </h2>
              <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.9rem', lineHeight: '1.6' }}>
                좌측 사이드바의 <strong style={{ color: 'white' }}>'바이어 정보 설정'</strong>에서 다음 바이어의 이름과 회사명을 입력하신 후 <strong style={{ color: 'var(--color-accent)' }}>[미팅 시작]</strong> 버튼을 누르면 15분 상담이 즉시 시작됩니다.
              </p>
            </div>
            <div style={{ border: '1px dashed var(--border-color)', borderRadius: '12px', padding: '1rem', backgroundColor: 'rgba(255,255,255,0.01)', width: '100%', maxWidth: '400px' }}>
              <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', display: 'block', marginBottom: '0.25rem' }}>현재 대화방 번호</span>
              <span style={{ fontSize: '1.5rem', fontWeight: 'bold', color: 'white', letterSpacing: '0.05em' }}>{roomId}</span>
            </div>
          </div>
        )}
      </div>

      {/* 3. Right dialogue history list */}
      <div className={styles.rightPanel}>
        <HistoryArea
          roomId={roomId}
          sessionData={getDisplaySessionData()}
          history={getDisplayHistory()}
          activeCompletedSessionId={activeCompletedSessionId}
        />
      </div>
    </div>
  );
}

export default function RoomPage() {
  return (
    <Suspense fallback={
      <div style={{ display: 'flex', height: '100vh', justifyContent: 'center', alignItems: 'center', backgroundColor: 'var(--bg-dark)' }}>
        <p style={{ color: 'var(--color-text-secondary)' }}>회의실 진입 중...</p>
      </div>
    }>
      <RoomContent />
    </Suspense>
  );
}
