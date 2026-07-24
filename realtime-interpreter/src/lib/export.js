import { LANGUAGES } from '../components/Sidebar';

export function exportSessionToHtml({ roomId, sessionData, history }) {
  const { buyerInfo = {}, sellerKeywords = '', languages = { seller: 'ko-KR', buyer: 'zh-CN' } } = sessionData || {};

  const sellerLangName = LANGUAGES.find(l => l.code === languages.seller)?.name || languages.seller;
  const buyerLangName = LANGUAGES.find(l => l.code === languages.buyer)?.name || languages.buyer;

  const formattedHistory = history.map((item, index) => {
    const time = new Date(item.timestamp).toLocaleTimeString();
    const speakerLabel = item.speaker === 'seller' ? '셀러 (Seller)' : (buyerInfo.name || '바이어 (Buyer)');
    const speakerClass = item.speaker === 'seller' ? 'seller-row' : 'buyer-row';
    const translationClass = item.speaker === 'seller' ? 'seller-translation' : 'buyer-translation';

    return `
      <tr class="dialog-row ${speakerClass}">
        <td class="time-cell">${time}</td>
        <td class="speaker-cell"><span class="speaker-badge">${speakerLabel}</span></td>
        <td class="text-cell">
          <div class="original-text">${item.originalText}</div>
          <div class="translated-text ${translationClass}">${item.translatedText}</div>
        </td>
      </tr>
    `;
  }).join('');

  const htmlContent = `<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>미팅 상담 기록 - Room ${roomId}</title>
  <style>
    :root {
      --bg-dark: #0f172a;
      --bg-card: #1e293b;
      --color-text-primary: #f8fafc;
      --color-text-secondary: #94a3b8;
      --color-accent: #38bdf8;
      --color-success: #10b981;
      --border-color: #334155;
    }
    
    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }
    
    body {
      background-color: var(--bg-dark);
      color: var(--color-text-primary);
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      padding: 2rem;
      line-height: 1.5;
    }
    
    .container {
      max-width: 900px;
      margin: 0 auto;
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: 16px;
      padding: 2.5rem;
      box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.3);
    }
    
    header {
      border-bottom: 2px solid var(--border-color);
      padding-bottom: 1.5rem;
      margin-bottom: 2rem;
    }
    
    .title-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 1rem;
    }
    
    h1 {
      font-size: 2rem;
      color: var(--color-accent);
      font-weight: 800;
      letter-spacing: -0.025em;
    }
    
    .room-badge {
      background: rgba(56, 189, 248, 0.1);
      border: 1px solid rgba(56, 189, 248, 0.3);
      color: var(--color-accent);
      padding: 0.35rem 0.75rem;
      border-radius: 20px;
      font-size: 0.85rem;
      font-weight: 700;
    }
    
    .meta-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1.5rem;
      margin-top: 1.5rem;
      background: rgba(0, 0, 0, 0.15);
      border-radius: 8px;
      padding: 1.25rem;
    }
    
    .meta-item {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }
    
    .meta-label {
      font-size: 0.75rem;
      color: var(--color-text-secondary);
      text-transform: uppercase;
      font-weight: 700;
      letter-spacing: 0.05em;
    }
    
    .meta-value {
      font-size: 0.95rem;
      font-weight: 500;
    }
    
    .memo-section {
      margin-top: 1.5rem;
      background: rgba(255, 255, 255, 0.02);
      border: 1px solid var(--border-color);
      border-radius: 8px;
      padding: 1.25rem;
    }
    
    .memo-title {
      font-size: 0.8rem;
      font-weight: 700;
      text-transform: uppercase;
      color: var(--color-text-secondary);
      margin-bottom: 0.5rem;
    }
    
    .memo-content {
      font-size: 0.9rem;
      white-space: pre-wrap;
      color: var(--color-text-primary);
    }
    
    table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 2rem;
    }
    
    th {
      text-align: left;
      padding: 1rem;
      border-bottom: 2px solid var(--border-color);
      color: var(--color-text-secondary);
      font-size: 0.8rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    
    td {
      padding: 1.25rem 1rem;
      border-bottom: 1px solid var(--border-color);
      vertical-align: top;
    }
    
    .time-cell {
      width: 100px;
      font-size: 0.8rem;
      color: var(--color-text-secondary);
      font-family: monospace;
    }
    
    .speaker-cell {
      width: 150px;
    }
    
    .speaker-badge {
      display: inline-block;
      padding: 0.25rem 0.5rem;
      border-radius: 4px;
      font-size: 0.75rem;
      font-weight: 700;
      text-transform: uppercase;
    }
    
    .seller-row .speaker-badge {
      background: rgba(56, 189, 248, 0.1);
      border: 1px solid rgba(56, 189, 248, 0.2);
      color: var(--color-accent);
    }
    
    .buyer-row .speaker-badge {
      background: rgba(16, 185, 129, 0.1);
      border: 1px solid rgba(16, 185, 129, 0.2);
      color: var(--color-success);
    }
    
    .original-text {
      font-size: 0.95rem;
      color: var(--color-text-primary);
      margin-bottom: 0.5rem;
    }
    
    .translated-text {
      font-size: 0.9rem;
      font-weight: 500;
    }
    
    .seller-translation {
      color: var(--color-accent);
    }
    
    .buyer-translation {
      color: var(--color-success);
    }
    
    .empty-state {
      text-align: center;
      padding: 3rem 0;
      color: var(--color-text-secondary);
      font-style: italic;
    }
    
    footer {
      margin-top: 3rem;
      text-align: center;
      font-size: 0.75rem;
      color: var(--color-text-secondary);
      border-top: 1px solid var(--border-color);
      padding-top: 1.5rem;
    }
  </style>
</head>
<body>
  <div class="container">
    <header>
      <div class="title-row">
        <h1>미팅 상담 기록</h1>
        <div class="room-badge">ROOM ${roomId}</div>
      </div>
      
      <div class="meta-grid">
        <div class="meta-item">
          <div class="meta-label">바이어 정보 (Buyer)</div>
          <div class="meta-value">${buyerInfo.name || '미등록'} (${buyerInfo.company || '회사 미등록'})</div>
        </div>
        <div class="meta-item">
          <div class="meta-label">기록 일시</div>
          <div class="meta-value">${new Date().toLocaleString()}</div>
        </div>
        <div class="meta-item">
          <div class="meta-label">셀러 언어 (Seller)</div>
          <div class="meta-value">${sellerLangName}</div>
        </div>
        <div class="meta-item">
          <div class="meta-label">바이어 언어 (Buyer)</div>
          <div class="meta-value">${buyerLangName}</div>
        </div>
      </div>
      
      ${sellerKeywords ? `
        <div class="memo-section">
          <div class="memo-title">상담 주제 및 키워드 메모</div>
          <div class="memo-content">${sellerKeywords}</div>
        </div>
      ` : ''}
    </header>
    
    <main>
      ${history.length === 0 ? `
        <div class="empty-state">통역 대화 기록이 존재하지 않습니다.</div>
      ` : `
        <table>
          <thead>
            <tr>
              <th>시간</th>
              <th>화자</th>
              <th>대화 및 통역 내용</th>
            </tr>
          </thead>
          <tbody>
            ${formattedHistory}
          </tbody>
        </table>
      `}
    </main>
    
    <footer>
      Gemini AI 기반 실시간 자동 번역(통역) 시스템 상담 완료 보고서
    </footer>
  </div>
</body>
</html>`;

  // Trigger HTML download in the client browser
  const blob = new Blob([htmlContent], { type: 'text/html;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  
  const buyerNameSanitized = (buyerInfo.name || 'buyer').replace(/[^a-z0-9가-힣]/gi, '_');
  
  // Format date as YYYYMMDD
  const now = new Date();
  const yyyy = now.getFullYear();
  const mm = String(now.getMonth() + 1).padStart(2, '0');
  const dd = String(now.getDate()).padStart(2, '0');
  const yyyymmdd = `${yyyy}${mm}${dd}`;

  link.download = `Meeting_Log_${buyerNameSanitized}_${yyyymmdd}.html`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
