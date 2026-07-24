import React, { useState, useEffect } from 'react';
import styles from './Components.module.css';
import { updateSession } from '../lib/sync';
import { exportSessionToHtml } from '../lib/export';

export const LANGUAGES = [
  { code: 'ko-KR', name: '한국어 (Korean)' },
  { code: 'en-US', name: '영어 (English)' },
  { code: 'ja-JP', name: '일본어 (Japanese)' },
  { code: 'zh-CN', name: '중국어 (Chinese Simplified)' },
  { code: 'zh-TW', name: '대만어 (Chinese Traditional)' },
  { code: 'es-ES', name: '스페인어 (Spanish)' },
  { code: 'fr-FR', name: '프랑스어 (French)' },
  { code: 'de-DE', name: '독일어 (German)' },
  { code: 'vi-VN', name: '베트남어 (Vietnamese)' },
  { code: 'th-TH', name: '태국어 (Thai)' },
  { code: 'id-ID', name: '인도네시아어 (Indonesian)' },
  { code: 'ru-RU', name: '러시아어 (Russian)' },
  { code: 'it-IT', name: '이탈리아어 (Italiano)' },
  { code: 'pt-PT', name: '포르투갈어 (Português)' },
];

export default function Sidebar({ 
  roomId, 
  sessionData, 
  role, 
  onEndSession, 
  completedSessions, 
  onViewCompletedSession, 
  activeCompletedSessionId, 
  onBackToLive,
  onStartNewMeeting
}) {
  const { buyerInfo = {}, sellerKeywords = '', status = 'ready', languages = { seller: 'ko-KR', buyer: 'zh-CN' } } = sessionData || {};
  
  const [sellerLang, setSellerLang] = useState(languages.seller);
  const [buyerLang, setBuyerLang] = useState(languages.buyer);
  const [keywords, setKeywords] = useState(sellerKeywords);

  // States for new buyer input fields (in place of glossary)
  const [buyerNameInput, setBuyerNameInput] = useState('');
  const [buyerCompanyInput, setBuyerCompanyInput] = useState('');
  const [buyerExtraInput, setBuyerExtraInput] = useState('');
  
  // Timer state
  const [elapsedSeconds, setElapsedSeconds] = useState(0);

  // Sync language selection states with database updates
  useEffect(() => {
    if (languages.seller) setSellerLang(languages.seller);
    if (languages.buyer) setBuyerLang(languages.buyer);
  }, [languages.seller, languages.buyer]);

  // Sync buyer inputs with active session metadata
  useEffect(() => {
    if (status === 'ready' || status === 'ended') {
      setBuyerNameInput('');
      setBuyerCompanyInput('');
      setBuyerExtraInput('');
    } else {
      setBuyerNameInput(buyerInfo.name || '');
      setBuyerCompanyInput(buyerInfo.company || '');
      setBuyerExtraInput(buyerInfo.extra || '');
    }
  }, [status, buyerInfo.name, buyerInfo.company, buyerInfo.extra]);

  useEffect(() => {
    if (sellerKeywords !== undefined) setKeywords(sellerKeywords);
  }, [sellerKeywords]);

  // Session Timer ticking logic
  useEffect(() => {
    let interval = null;
    if (status === 'active') {
      const startTime = sessionData.timerStartTime || Date.now();
      
      interval = setInterval(() => {
        const elapsed = Math.floor((Date.now() - startTime) / 1000);
        setElapsedSeconds(elapsed >= 0 ? elapsed : 0);
      }, 1000);
    } else if (status === 'ended') {
      setElapsedSeconds(sessionData.timerDuration || 0);
    } else {
      setElapsedSeconds(0);
    }

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [status, sessionData.timerStartTime, sessionData.timerDuration]);

  // Format seconds to 00:00:00 or 00:00
  const formatTime = (totalSecs) => {
    const hrs = Math.floor(totalSecs / 3600);
    const mins = Math.floor((totalSecs % 3600) / 60);
    const secs = totalSecs % 60;
    
    const pad = (n) => String(n).padStart(2, '0');
    return `${pad(hrs)}:${pad(mins)}:${pad(secs)}`;
  };

  const handleSaveLanguages = async () => {
    await updateSession(roomId, {
      languages: { seller: sellerLang, buyer: buyerLang }
    });
    alert('언어 설정이 적용되었습니다.');
  };

  const handleSaveKeywords = async () => {
    await updateSession(roomId, { sellerKeywords: keywords });
    alert('상담 주제 및 메모가 저장되었습니다.');
  };

  const handleStartMeetingSubmit = (e) => {
    e.preventDefault();
    if (!buyerNameInput.trim() || !buyerCompanyInput.trim()) {
      alert('바이어 이름과 회사명을 입력해주세요.');
      return;
    }
    if (onStartNewMeeting) {
      onStartNewMeeting({
        name: buyerNameInput.trim(),
        company: buyerCompanyInput.trim(),
        extra: buyerExtraInput.trim()
      });
    }
  };

  return (
    <div className={styles.sidebar}>
      {/* Sidebar Header */}
      <div className={styles.sidebarHeader}>
        <h2>자동통역 운영자 관리</h2>
        <p>Room ID: {roomId} ({role === 'seller' ? '셀러 화면' : '바이어 화면'})</p>
      </div>

      {/* Language Selection */}
      <div className={styles.section}>
        <span className={styles.sectionTitle}>언어 설정</span>
        <div className={styles.formGroup}>
          <label>셀러 (회의 주체) 언어</label>
          <select 
            className={styles.select} 
            value={sellerLang} 
            onChange={(e) => setSellerLang(e.target.value)}
            disabled={role !== 'seller'}
          >
            {LANGUAGES.map(lang => (
              <option key={lang.code} value={lang.code}>{lang.name}</option>
            ))}
          </select>
        </div>
        <div className={styles.formGroup}>
          <label>바이어 (상대방) 언어</label>
          <select 
            className={styles.select} 
            value={buyerLang} 
            onChange={(e) => setBuyerLang(e.target.value)}
            disabled={role !== 'seller'}
          >
            {LANGUAGES.map(lang => (
              <option key={lang.code} value={lang.code}>{lang.name}</option>
            ))}
          </select>
        </div>
        {role === 'seller' && (
          <button className={styles.button} onClick={handleSaveLanguages}>저장</button>
        )}
      </div>

      {/* Keywords / Memo Space */}
      <div className={styles.section}>
        <span className={styles.sectionTitle}>상담 주제 및 메모</span>
        <div className={styles.formGroup}>
          <textarea
            className={styles.textarea}
            placeholder="회의 핵심 키워드나 주제를 메모하세요..."
            value={keywords}
            onChange={(e) => setKeywords(e.target.value)}
            disabled={role !== 'seller'}
          />
        </div>
        {role === 'seller' && (
          <button className={styles.button} onClick={handleSaveKeywords}>저장</button>
        )}
      </div>

      {/* Buyer Information Configuration (Replaces Glossary dictionary) */}
      <div className={styles.section}>
        <span className={styles.sectionTitle}>바이어 정보 설정</span>
        {status === 'active' ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', fontSize: '0.85rem' }}>
            <div>
              <span style={{ color: 'var(--color-text-secondary)', fontWeight: 600 }}>바이어 이름:</span>{' '}
              <span style={{ color: 'var(--color-text-primary)' }}>{buyerInfo.name || '미등록'}</span>
            </div>
            <div>
              <span style={{ color: 'var(--color-text-secondary)', fontWeight: 600 }}>회사명:</span>{' '}
              <span style={{ color: 'var(--color-text-primary)' }}>{buyerInfo.company || '미등록'}</span>
            </div>
            {buyerInfo.extra && (
              <div>
                <span style={{ color: 'var(--color-text-secondary)', fontWeight: 600 }}>특이사항:</span>
                <p style={{ color: 'var(--color-text-muted)', fontSize: '0.75rem', marginTop: '0.2rem', whiteSpace: 'pre-wrap', lineHeight: '1.4' }}>
                  {buyerInfo.extra}
                </p>
              </div>
            )}
          </div>
        ) : (
          role === 'seller' ? (
            <form onSubmit={handleStartMeetingSubmit} className={styles.formGroup} style={{ gap: '0.5rem' }}>
              <input 
                type="text" 
                placeholder="바이어 이름" 
                className={styles.input}
                value={buyerNameInput}
                onChange={(e) => setBuyerNameInput(e.target.value)}
                required
              />
              <input 
                type="text" 
                placeholder="바이어 회사명" 
                className={styles.input}
                value={buyerCompanyInput}
                onChange={(e) => setBuyerCompanyInput(e.target.value)}
                required
              />
              <textarea 
                placeholder="특이사항 및 메모 (선택)" 
                className={styles.textarea}
                style={{ minHeight: '60px', fontSize: '0.8rem', resize: 'none' }}
                value={buyerExtraInput}
                onChange={(e) => setBuyerExtraInput(e.target.value)}
              />
              <button type="submit" className={styles.button} style={{ width: '100%', padding: '0.6rem', marginTop: '0.25rem' }}>
                미팅 시작 (회의 활성화)
              </button>
            </form>
          ) : (
            <p style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', fontStyle: 'italic', textAlign: 'center' }}>
              셀러가 새로운 미팅을 준비하고 있습니다...
            </p>
          )
        )}
      </div>

      {/* Consultation Session (Timer & Status) */}
      <div className={styles.section}>
        <span className={styles.sectionTitle}>상담 세션</span>
        <div className={styles.timerCard}>
          <div className={styles.timerValue}>
            {formatTime(elapsedSeconds)}
          </div>
          <div className={styles.timerStatus}>
            <div className={`${styles.statusIndicator} ${
              status === 'active' ? styles.statusIndicatorActive : 
              status === 'ended' ? styles.statusIndicatorEnded : ''
            }`} />
            <span style={{ fontWeight: 600 }}>
              {status === 'ready' && '대기 중'}
              {status === 'active' && '진행 중'}
              {status === 'ended' && '회의 종료'}
            </span>
          </div>
          {status === 'active' && (
            <p style={{ fontSize: '0.7rem', color: 'var(--color-text-muted)', textAlign: 'center', marginTop: '0.25rem' }}>
              시작 시각: {new Date(sessionData.timerStartTime || Date.now()).toLocaleTimeString()}
            </p>
          )}
        </div>
        {role === 'seller' && status === 'active' && (
          <button 
            className={`${styles.button} ${styles.buttonDanger}`} 
            onClick={onEndSession}
          >
            세션 종료
          </button>
        )}
      </div>

      {/* Completed Sessions (상담 기록 - 종료) */}
      <div className={styles.section} style={{ borderBottom: 'none' }}>
        <span className={styles.sectionTitle}>완료된 세션</span>
        {activeCompletedSessionId && (
          <button 
            className={`${styles.button} ${styles.buttonSecondary}`}
            onClick={onBackToLive}
            style={{ padding: '0.4rem', fontSize: '0.75rem', marginBottom: '0.5rem' }}
          >
            ← 라이브 회의로 돌아가기
          </button>
        )}
        <div className={styles.sessionList}>
          {completedSessions.length === 0 ? (
            <p style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', fontStyle: 'italic', textAlign: 'center' }}>
              완료된 세션 기록이 없습니다.
            </p>
          ) : (
            completedSessions.map((session) => (
              <div 
                key={session.id} 
                className={styles.sessionItem}
                style={{
                  borderColor: activeCompletedSessionId === session.id ? 'var(--color-accent)' : 'var(--border-color)',
                  background: activeCompletedSessionId === session.id ? 'rgba(56, 189, 248, 0.05)' : 'rgba(255, 255, 255, 0.02)'
                }}
              >
                <div className={styles.sessionItemInfo}>
                  <span className={styles.sessionItemTitle}>{session.title || '이름 없음'}</span>
                  <span className={styles.sessionItemMeta}>
                    {session.date} | {formatTime(session.duration)}
                  </span>
                </div>
                <div style={{ display: 'flex', gap: '0.6rem' }}>
                  <button 
                    className={styles.viewBtn} 
                    onClick={() => onViewCompletedSession(session)}
                  >
                    보기
                  </button>
                  <button 
                    className={styles.viewBtn} 
                    style={{ color: 'var(--color-success)' }}
                    onClick={() => {
                      exportSessionToHtml({
                        roomId,
                        sessionData: {
                          buyerInfo: session.buyerInfo,
                          sellerKeywords: session.keywords,
                          languages: session.languages
                        },
                        history: session.history
                      });
                    }}
                  >
                    출력
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
