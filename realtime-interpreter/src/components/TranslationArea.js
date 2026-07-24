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

  // Bind local audio visualizer to local user's active speaking transcript channel
  const myLiveTranscript = role === 'seller' ? liveTranscriptSeller : liveTranscriptBuyer;

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

      {/* Main Dual-channel Translation Cards Stream */}
      <div className={styles.streamContainer} style={{ gap: '1.25rem' }}>
        <span style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          양방향 실시간 동시 통역 채널
        </span>

        {/* 1. Seller's Live speech translation box */}
        <div 
          className={`glass-panel ${styles.streamCard} ${liveTranscriptSeller ? styles.streamCardActive : ''}`}
          style={{ width: '100%', maxWidth: '600px', padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.75rem', borderRadius: '12px' }}
        >
          <div className={styles.speakerMeta}>
            <span className={styles.roleTag}>Seller</span>
            <span className={styles.langMeta}>{sellerLangName} → {buyerLangName}</span>
          </div>

          <div className={styles.originalText} style={{ fontSize: '1.15rem', minHeight: '2.5rem', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            {liveTranscriptSeller ? (
              liveTranscriptSeller.originalText || '말씀하시는 중...'
            ) : (
              <span style={{ color: 'var(--color-text-muted)', fontSize: '0.9rem', fontStyle: 'italic' }}>
                셀러 대기 중 (Seller Silent)
              </span>
            )}
          </div>

          <div 
            className={styles.translatedText}
            style={{ fontSize: '1rem', minHeight: '2.2rem', paddingTop: '0.75rem', borderTop: '1px dashed var(--border-color)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
          >
            {liveTranscriptSeller ? (
              liveTranscriptSeller.translatedText || <span className={styles.translationLoading} style={{ fontSize: '0.9rem' }}>번역 분석 중...</span>
            ) : (
              <span style={{ color: 'var(--color-text-muted)', fontSize: '0.8rem' }}>
                번역 내용이 이곳에 표시됩니다.
              </span>
            )}
          </div>
        </div>

        {/* 2. Buyer's Live speech translation box */}
        <div 
          className={`glass-panel ${styles.streamCard} ${liveTranscriptBuyer ? styles.streamCardActiveBuyer : ''}`}
          style={{ width: '100%', maxWidth: '600px', padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.75rem', borderRadius: '12px' }}
        >
          <div className={styles.speakerMeta}>
            <span className={`${styles.roleTag} ${styles.roleTagBuyer}`}>Buyer</span>
            <span className={styles.langMeta}>{buyerLangName} → {sellerLangName}</span>
          </div>

          <div className={styles.originalText} style={{ fontSize: '1.15rem', minHeight: '2.5rem', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            {liveTranscriptBuyer ? (
              liveTranscriptBuyer.originalText || '말씀하시는 중...'
            ) : (
              <span style={{ color: 'var(--color-text-muted)', fontSize: '0.9rem', fontStyle: 'italic' }}>
                {buyerInfo.name || '바이어'} 대기 중 (Buyer Silent)
              </span>
            )}
          </div>

          <div 
            className={`${styles.translatedText} ${styles.translatedTextBuyer}`}
            style={{ fontSize: '1rem', minHeight: '2.2rem', paddingTop: '0.75rem', borderTop: '1px dashed var(--border-color)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
          >
            {liveTranscriptBuyer ? (
              liveTranscriptBuyer.translatedText || <span className={styles.translationLoading} style={{ fontSize: '0.9rem' }}>번역 분석 중...</span>
            ) : (
              <span style={{ color: 'var(--color-text-muted)', fontSize: '0.8rem' }}>
                번역 내용이 이곳에 표시됩니다.
              </span>
            )}
          </div>
        </div>

        {/* Live Audio Visualizer feedback */}
        <div style={{ width: '200px', height: '24px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <AudioVisualizer isActive={isListening && myLiveTranscript?.originalText?.length > 0} />
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
              padding: '0.8rem 1.8rem',
              borderRadius: '30px',
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem',
              fontWeight: 700,
              fontSize: '0.9rem',
              boxShadow: isListening ? '0 0 15px rgba(239, 68, 68, 0.4)' : '0 4px 12px rgba(56, 189, 248, 0.2)',
              opacity: isMeetingActive ? 1 : 0.5,
              transition: 'all 0.2s ease'
            }}
          >
            <span>🎙️</span>
            {isListening ? '내 마이크 끄기' : '내 마이크 켜기'}
          </button>
          
          <div style={{ height: '24px', display: 'flex', alignItems: 'center', justifyContent: 'center', marginTop: '0.25rem' }}>
            {micError ? (
              <p style={{ color: 'var(--color-live)', fontSize: '0.75rem', margin: 0 }}>
                {micError}
              </p>
            ) : isListening ? (
              <p style={{ color: 'var(--color-success)', fontSize: '0.75rem', fontWeight: 500, margin: 0 }}>
                내 마이크가 켜져 있습니다. 말하면 실시간 번역됩니다.
              </p>
            ) : (
              <p style={{ color: 'var(--color-text-muted)', fontSize: '0.75rem', margin: 0 }}>
                상대방과 대화하려면 마이크를 켜 주세요.
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
