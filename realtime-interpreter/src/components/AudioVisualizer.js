import React from 'react';
import styles from './Components.module.css';

export default function AudioVisualizer({ isActive }) {
  // Generate a set of bars with random animation delays for a natural voice wave effect
  const barCount = 10;
  
  return (
    <div className={styles.visualizerContainer}>
      {Array.from({ length: barCount }).map((_, i) => {
        const heightPercent = 20 + Math.random() * 80;
        const animationDelay = `${i * 0.1}s`;
        const animationDuration = `${0.6 + Math.random() * 0.8}s`;

        return (
          <div
            key={i}
            className={styles.visualizerBar}
            style={{
              height: isActive ? `${heightPercent}%` : '4px',
              animationDelay: isActive ? animationDelay : '0s',
              animationDuration: isActive ? animationDuration : '0s',
              animationPlayState: isActive ? 'running' : 'paused',
              backgroundColor: isActive ? 'var(--color-accent)' : 'var(--color-text-muted)',
              transition: 'height 0.3s ease, background-color 0.3s ease'
            }}
          />
        );
      })}
    </div>
  );
}
