'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import styles from './room/Room.module.css';
import compStyles from '../components/Components.module.css';
import { updateSession } from '../lib/sync';

export default function Lobby() {
  const router = useRouter();
  const [roomId, setRoomId] = useState('');
  const [role, setRole] = useState('seller'); // seller or buyer
  const [buyerName, setBuyerName] = useState('');
  const [buyerCompany, setBuyerCompany] = useState('');
  const [buyerExtra, setBuyerExtra] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!roomId.trim()) {
      setError('방 번호(Room ID)를 입력해주세요.');
      return;
    }

    setIsLoading(true);
    setError('');

    const formattedRoomId = roomId.trim();

    try {
      // If we are a Seller, we initialize the session state in the sync database
      if (role === 'seller') {
        const initialSession = {
          buyerInfo: {
            name: buyerName.trim() || '미팅 바이어',
            company: buyerCompany.trim() || '바이어 회사',
            extra: buyerExtra.trim()
          },
          sellerKeywords: '',
          status: 'active', // Set to active immediately on Seller start
          languages: { seller: 'ko-KR', buyer: 'zh-CN' },
          liveTranscript: null,
          glossary: [],
          timerStartTime: Date.now(),
          timerDuration: 0
        };

        // Write initial session to shared database (local storage or Firebase)
        await updateSession(formattedRoomId, initialSession);
      }

      // Redirect to room
      router.push(`/room?room=${formattedRoomId}&role=${role}`);
    } catch (err) {
      console.error(err);
      setError('회의를 초기화하는 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className={styles.lobbyContainer}>
      <div className={`glass-panel ${styles.lobbyCard}`}>
        <h1 className={styles.lobbyTitle}>Gemini AI 자동통역</h1>
        <p className={styles.lobbySubtitle}>실시간 언어 번역 및 화면 동기화 솔루션</p>

        <form onSubmit={handleSubmit} className={styles.lobbyForm}>
          {/* Room ID Input */}
          <div className={compStyles.formGroup}>
            <label style={{ fontWeight: 600 }}>방 번호 (Room ID)</label>
            <input
              type="text"
              className={compStyles.input}
              placeholder="예: 5675"
              value={roomId}
              onChange={(e) => setRoomId(e.target.value)}
              required
            />
          </div>

          {/* Role Selection */}
          <div className={compStyles.formGroup}>
            <label style={{ fontWeight: 600 }}>본인의 역할</label>
            <div className={styles.roleSelectContainer}>
              <div 
                className={`${styles.roleBox} ${role === 'seller' ? styles.roleBoxActive : ''}`}
                onClick={() => setRole('seller')}
              >
                <span className={styles.roleIcon}>💼</span>
                <span className={styles.roleTitle}>셀러 (회의 개설)</span>
              </div>
              <div 
                className={`${styles.roleBox} ${role === 'buyer' ? (buyerName ? styles.roleBoxActiveBuyer : styles.roleBoxActive) : ''}`}
                onClick={() => setRole('buyer')}
              >
                <span className={styles.roleIcon}>🤝</span>
                <span className={styles.roleTitle}>바이어 (회의 참가)</span>
              </div>
            </div>
          </div>

          {/* Buyer Information - Only show or require details if Seller is setting up, or optional for Buyer */}
          {role === 'seller' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginTop: '0.5rem', borderTop: '1px solid var(--border-color)', paddingTop: '1rem' }}>
              <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--color-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                바이어 (상대방) 정보 입력
              </span>
              <div className={compStyles.formGroup}>
                <label>바이어 이름 (Buyer Name)</label>
                <input
                  type="text"
                  className={compStyles.input}
                  placeholder="예: 왕웨이 (Wang Wei)"
                  value={buyerName}
                  onChange={(e) => setBuyerName(e.target.value)}
                  required
                />
              </div>
              <div className={compStyles.formGroup}>
                <label>바이어 회사명 (Company)</label>
                <input
                  type="text"
                  className={compStyles.input}
                  placeholder="예: 텐센트 테크놀로지"
                  value={buyerCompany}
                  onChange={(e) => setBuyerCompany(e.target.value)}
                  required
                />
              </div>
              <div className={compStyles.formGroup}>
                <label>미팅 관련 추가 정보 / 메모 (선택)</label>
                <textarea
                  className={compStyles.textarea}
                  placeholder="예: 뷰티 신기술 도입 및 크로스보더 협업 미팅"
                  value={buyerExtra}
                  onChange={(e) => setBuyerExtra(e.target.value)}
                />
              </div>
            </div>
          )}

          {error && (
            <p style={{ color: 'var(--color-live)', fontSize: '0.85rem', textAlign: 'center' }}>
              {error}
            </p>
          )}

          <button 
            type="submit" 
            className={compStyles.button} 
            disabled={isLoading}
            style={{ width: '100%', padding: '1rem', fontSize: '1rem', marginTop: '1rem' }}
          >
            {isLoading ? '설정 중...' : role === 'seller' ? '미팅 시작 (회의실 개설)' : '회의 입장'}
          </button>
        </form>
      </div>
    </div>
  );
}
