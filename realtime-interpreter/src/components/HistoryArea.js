import React, { useEffect, useRef } from 'react';
import styles from './Components.module.css';
import { exportSessionToHtml } from '../lib/export';

export default function HistoryArea({ 
  roomId, 
  sessionData, 
  history, 
  activeCompletedSessionId 
}) {
  const { buyerInfo = {} } = sessionData || {};
  const listRef = useRef(null);

  // Auto-scroll to top of dialogue history on new items (since newest is now at the top)
  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = 0;
    }
  }, [history]);

  const handleExport = () => {
    if (history.length === 0) {
      alert('출력할 대화 기록이 없습니다.');
      return;
    }
    exportSessionToHtml({ roomId, sessionData, history });
  };

  // Reverse history so that the newest conversations appear at the top
  const reversedHistory = [...history].reverse();

  return (
    <div className={styles.historyArea}>
      {/* Header */}
      <div className={styles.historyHeader}>
        <h2>대화 기록 (Dialogue Logs)</h2>
        <button 
          className={`${styles.button} ${styles.buttonSecondary}`} 
          style={{ padding: '0.4rem 0.8rem', fontSize: '0.75rem' }}
          onClick={handleExport}
        >
          HTML 출력
        </button>
      </div>

      {/* History List */}
      {history.length === 0 ? (
        <div className={styles.emptyHistory}>
          기록된 대화가 없습니다.
        </div>
      ) : (
        <div className={styles.historyList} ref={listRef}>
          {reversedHistory.map((item, idx) => {
            const timeString = new Date(item.timestamp).toLocaleTimeString([], {
              hour: '2-digit',
              minute: '2-digit',
              second: '2-digit'
            });
            const isBuyer = item.speaker === 'buyer';
            const speakerLabel = isBuyer ? (buyerInfo.name || '바이어') : '셀러';
            
            // In the reversed list, the latest item is the first element (index 0)
            const isLatest = idx === 0 && !activeCompletedSessionId;

            return (
              <div 
                key={item.id || idx} 
                className={styles.historyItem}
                style={
                  isLatest ? {
                    border: isBuyer ? '1px solid var(--color-success)' : '1px solid var(--color-accent)',
                    boxShadow: isBuyer ? '0 0 12px rgba(16, 185, 129, 0.2)' : '0 0 12px rgba(56, 189, 248, 0.2)',
                  } : {}
                }
              >
                <div className={styles.itemHeader}>
                  <div className={styles.itemRoleAndName}>
                    <span className={`${styles.roleTag} ${isBuyer ? styles.roleTagBuyer : ''}`}>
                      {isBuyer ? 'Buyer' : 'Seller'}
                    </span>
                    <span className={styles.itemSpeakerName}>{speakerLabel}</span>
                  </div>
                  <span className={styles.itemTime}>{timeString}</span>
                </div>
                
                <div className={styles.itemOriginal}>
                  {item.originalText}
                </div>
                
                <div className={`${styles.itemTranslated} ${isBuyer ? styles.itemTranslatedBuyer : ''}`}>
                  {item.translatedText}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
