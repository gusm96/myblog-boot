import type { Metadata } from "next";
import Script from "next/script";
import { Providers } from "@/providers/Providers";
import "bootstrap/dist/css/bootstrap.min.css";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "MyBlog",
    template: "%s | MyBlog",
  },
  description: "개발 블로그 — 기술 지식을 공유합니다.",
  // 서치 콘솔 인증: 아래 값을 실제 코드로 교체
  // verification: {
  //   google: "GOOGLE_SEARCH_CONSOLE_CODE",
  //   other: {
  //     "naver-site-verification": "NAVER_WEBMASTER_CODE",
  //   },
  // },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>
        <Providers>{children}</Providers>
        {/* Font Awesome kit */}
        <Script
          src="https://kit.fontawesome.com/9cdfdf3db8.js"
          crossOrigin="anonymous"
          strategy="lazyOnload"
        />
      </body>
    </html>
  );
}
