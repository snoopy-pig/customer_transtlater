'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import styles from './room/Room.module.css';
import compStyles from '../components/Components.module.css';
import { updateSession, getAllRooms, deleteRoom } from '../lib/sync';

export default function Lobby() {
  const router = useRouter();
  const [roomId, setRoomId] = useState('');
  const [role, setRole] = useState('seller'); // seller or buyer
  const [buyerName, setBuyerName] = useState('');
  const [buyerCompany, setBuyerCompany] = useState('');
  const [buyerExtra, setBuyerExtra] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // Room Manager state
  const [activeRooms, setActiveRooms] = useState([]);

  // Fetch list of active rooms on mount and periodically
  const fetchRooms = async () => {
    const rooms = await getAllRooms();
    setActiveRooms(rooms);
  };

  useEffect(() => {
    fetchRooms();
    const interval = setInterval(fetchRooms, 5000);
    return () => clearInterval(interval);
  }, []);

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
          liveTranscriptSeller: null,
          liveTranscriptBuyer: null,
          activeMic: null
        };

        // Write initial session to shared database
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

  const handleDeleteRoom = async (roomIdToDelete, e) => {
    e.stopPropagation();
    if (!window.confirm(`방 번호 ${roomIdToDelete}의 세션을 완전히 삭제하시겠습니까?`)) return;
    await deleteRoom(roomIdToDelete);
    await fetchRooms();
  };

  return (
    <div className={styles.lobbyContainer} style={{ flexDirection: 'column', gap: '2rem', padding: '2rem 1rem' }}>
      <div className={`glass-panel ${styles.lobbyCard}`} style={{ maxWidth: '600px', width: '100%' }}>
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

          {/* Buyer Information */}
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

      {/* Active Room Manager Panel */}
      <div className={`glass-panel ${styles.lobbyCard}`} style={{ maxWidth: '600px', width: '100%', padding: '1.5rem' }}>
        <h2 style={{ fontSize: '1.1rem', color: 'var(--color-accent)', margin: '0 0 1rem 0', display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700 }}>
          🛡️ 활성화된 방 관리 (Active Rooms Manager)
        </h2>
        
        {activeRooms.length === 0 ? (
          <p style={{ color: 'var(--color-text-muted)', fontSize: '0.8rem', fontStyle: 'italic', textAlign: 'center', margin: '1rem 0' }}>
            현재 개설된 회의실이 없습니다.
          </p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', maxHeight: '250px', overflowY: 'auto', paddingRight: '0.25rem' }}>
            {activeRooms.map((room) => {
              const { name = '미등록', company = '미등록' } = room.buyerInfo || {};
              const isEnded = room.status === 'ended';
              const isActive = room.status === 'active';
              return (
                <div 
                  key={room.id}
                  style={{
                    display: 'flex',
                    justify-content: 'space-between',
                    alignItems: 'center',
                    padding: '0.75rem 1rem',
                    borderRadius: '8px',
                    border: '1px solid var(--border-color)',
                    background: 'rgba(255, 255, 255, 0.02)',
                    transition: 'all 0.2s ease',
                    cursor: 'pointer'
                  }}
                  onClick={() => {
                    setRoomId(room.id);
                    setRole('buyer'); // Auto-configure role input on click!
                  }}
                >
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <span style={{ fontSize: '0.95rem', fontWeight: 700, color: 'white' }}>Room {room.id}</span>
                      <span 
                        style={{
                          fontSize: '0.65rem',
                          padding: '0.1rem 0.4rem',
                          borderRadius: '10px',
                          fontWeight: 600,
                          backgroundColor: isActive ? 'rgba(16, 185, 129, 0.15)' : (isEnded ? 'rgba(239, 68, 68, 0.15)' : 'rgba(255,255,255,0.1)'),
                          color: isActive ? 'var(--color-success)' : (isEnded ? 'var(--color-live)' : 'var(--color-text-muted)')
                        }}
                      >
                        {isActive ? '진행 중' : (isEnded ? '종료됨' : '대기 중')}
                      </span>
                    </div>
                    <span style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)' }}>
                      바이어: {name} ({company})
                    </span>
                  </div>
                  <button
                    onClick={(e) => handleDeleteRoom(room.id, e)}
                    style={{
                      padding: '0.35rem 0.75rem',
                      fontSize: '0.75rem',
                      borderRadius: '6px',
                      border: 'none',
                      backgroundColor: 'rgba(239, 68, 68, 0.15)',
                      color: '#ff5c5c',
                      cursor: 'pointer',
                      transition: 'all 0.2s ease',
                      fontWeight: 600
                    }}
                  >
                    방 삭제
                  </button>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
