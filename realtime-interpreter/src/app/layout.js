import "./globals.css";

export const metadata = {
  title: "Gemini AI 실시간 자동통역 시스템",
  description: "음성을 인식하고 실시간으로 통역 및 동기화해 주는 비즈니스 미팅 자동 통역 웹 애플리케이션",
};

export default function RootLayout({ children }) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}

