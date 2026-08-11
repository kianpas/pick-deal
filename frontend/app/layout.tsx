import type { Metadata } from "next";
import { themeInitScript } from "@/lib/theme";
import "./globals.css";

export const metadata: Metadata = {
  title: "PickDeal — 핫딜 모아보기",
  description: "여러 커뮤니티의 핫딜을 한 곳에서.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ko" suppressHydrationWarning>
      <head>
        {/* FOUC 방지: 페인트 직전에 .dark 클래스를 결정한다 */}
        <script dangerouslySetInnerHTML={{ __html: themeInitScript }} />

        {/*
         * 폰트는 Google Fonts CDN에서 받는다. next/font는 빌드 때 폰트 파일을 전부
         * 내려받는데, 한글 폰트는 유니코드 레인지가 100개를 넘어 다운로드가 불안정하고
         * 실패하면 빌드가 깨진다. 링크 방식은 빌드 의존성이 없고 브라우저가 실제 쓰는
         * 구간만 받아간다.
         */}
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link
          rel="stylesheet"
          href="https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@500;600;700&family=IBM+Plex+Sans+KR:wght@400;500;600;700&display=swap"
        />
      </head>
      <body className="min-h-screen antialiased">{children}</body>
    </html>
  );
}
