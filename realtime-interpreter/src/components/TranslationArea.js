import React from 'react';
import styles from './Components.module.css';
import AudioVisualizer from './AudioVisualizer';
import { LANGUAGES } from './Sidebar';

export default function TranslationArea({ 
  roomId, 
  sessionData, 
  role, 
  isListening, 
  onToggleMic, 
  micError 
}) {
  const { 
    buyerInfo = {}, 
    status = 'ready', 
    languages = { seller: 'ko-KR', buyer: 'zh-CN' },
    liveTranscriptSeller = null,
    liveTranscriptBuyer = null
  } = sessionData || {};

  const sellerLangName = LANGUAGES.find(l => l.code === languages.seller)?.name || languages.seller;
  const buyerLangName = LANGUAGES.find(l => l.code === languages.buyer)?.name || languages.buyer;

  const isMeetingActive = status === 'active';

  // Determine active speaker based on which channel contains data
  const liveTranscript = liveTranscriptSeller || liveTranscriptBuyer;
  const activeSpeaker = liveTranscriptSeller ? 'seller' : (liveTranscriptBuyer ? 'buyer' : null);
  const activeSpeakerName = activeSpeaker === 'seller' ? '셀러 (Seller)' : (buyerInfo.name || '바이어 (Buyer)');

  // Format target languages info header dynamically based on active speaker
  const getFlowHeader = () => {
    if (activeSpeaker === 'seller') {
      return `SELLER (${sellerLangName}) → BUYER (${buyerLangName})`;
    } else if (activeSpeaker === 'buyer') {
      return `BUYER (${buyerLangName}) → SELLER (${sellerLangName})`;
    }
    return `대기 중 - 대화를 시작하세요`;
  };

  return (
    <div className={styles.translationArea}>
      {/* Top Header */}
      <div className={styles.liveHeader}>
        <div className={styles.liveBadge} style={{ opacity: isMeetingActive ? 1 : 0.4 }}>
          <div className={isMeetingActive ? styles.liveDot : ''} style={{ backgroundColor: isMeetingActive ? 'var(--color-live)' : 'var(--color-text-muted)' }} />
          <span>{isMeetingActive ? 'LIVE' : 'WAITING'}</span>
        </div>
        <div className={styles.liveRoomId}>
          방 번호: <span style={{ fontWeight: 600, color: 'var(--color-accent)' }}>{roomId}</span>
        </div>
      </div>

      {/* Main Single Dynamic Translation Card */}
      <div className={styles.streamContainer}>
        <span style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.1em' }}>
          {getFlowHeader()}
        </span>

        <div className={`glass-panel ${styles.streamCard} ${liveTranscript ? (activeSpeaker === 'buyer' ? styles.streamCardActiveBuyer : styles.streamCardActive) : ''}`}>
          <div className={styles.speakerMeta}>
            <span className={`${styles.roleTag} ${activeSpeaker === 'buyer' ? styles.roleTagBuyer : ''}`}>
              {activeSpeaker ? (activeSpeaker === 'seller' ? 'Seller' : 'Buyer') : '대기 중'}
            </span>
            <span className={styles.langMeta}>
              {activeSpeaker ? activeSpeakerName : ''}
            </span>
          </div>

          <div className={styles.originalText}>
            {liveTranscript ? (
              liveTranscript.originalText || '말씀하시는 중...'
            ) : (
              <span style={{ color: 'var(--color-text-muted)', fontSize: '1.15rem' }}>
                {isMeetingActive ? '마이크를 켜고 음성을 입력하세요.' : '미팅이 시작되면 통역이 가능합니다.'}
              </span>
            )}
          </div>

          <div className={`${styles.translatedText} ${activeSpeaker === 'buyer' ? styles.translatedTextBuyer : ''}`}>
            {liveTranscript ? (
              liveTranscript.translatedText ? (
                liveTranscript.translatedText
              ) : (
                <span className={styles.translationLoading}>번역 분석 중...</span>
              )
            ) : (
              <span style={{ color: 'var(--color-text-muted)', fontSize: '0.95rem' }}>
                실시간 번역이 이곳에 표시됩니다.
              </span>
            )}
          </div>
        </div>

        {/* Live Audio Visualizer feedback */}
        <div style={{ width: '200px', height: '32px' }}>
          <AudioVisualizer isActive={isListening && liveTranscript?.originalText?.length > 0} />
        </div>
      </div>

      {/* Mic Toggle Button Section */}
      <div className={styles.liveFooter}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.5rem' }}>
          <button
            className={`${styles.button} ${isListening ? styles.buttonDanger : ''}`}
            onClick={onToggleMic}
            disabled={!isMeetingActive}
            style={{
              padding: '1rem 2rem',
              borderRadius: '30px',
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem',
              fontWeight: 700,
              boxShadow: isListening ? '0 0 15px rgba(239, 68, 68, 0.4)' : '0 4px 12px rgba(56, 189, 248, 0.2)',
              opacity: isMeetingActive ? 1 : 0.5
            }}
          >
            <span>🎙️</span>
            {isListening ? '음성 인식 종료' : '마이크 켜기 (발언하기)'}
          </button>
          
          <div style={{ height: '24px', display: 'flex', alignItems: 'center', justifyContent: 'center', marginTop: '0.25rem' }}>
            {micError ? (
              <p style={{ color: 'var(--color-live)', fontSize: '0.75rem', margin: 0 }}>
                {micError}
              </p>
            ) : isListening ? (
              <p style={{ color: 'var(--color-success)', fontSize: '0.75rem', fontWeight: 500, margin: 0 }}>
                음성을 말하면 실시간으로 분석하여 자동 변환됩니다.
              </p>
            ) : null}
          </div>
        </div>
      </div>
    </div>
  );
}
