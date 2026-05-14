import type { Metadata } from "next";
import Script from "next/script";
import { Noto_Sans_KR, JetBrains_Mono } from "next/font/google";
import { Providers } from "@/providers/Providers";
import { JsonLd } from "@/components/seo/JsonLd";
import { buildOrganizationSchema } from "@/lib/seo";
import "bootstrap/dist/css/bootstrap.min.css";
import "./globals.css";

const SITE_URL = (process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000").replace(/\/$/, "");

const sans = Noto_Sans_KR({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
  display: "swap",
  variable: "--font-sans",
});

const mono = JetBrains_Mono({
  subsets: ["latin"],
  weight: ["400", "500", "700"],
  display: "swap",
  variable: "--font-mono",
});

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: "Dev-Moya",
    template: "%s | Dev-Moya",
  },
  description: "개발 블로그 — 기술 지식을 공유합니다.",
  alternates: { canonical: "/" },
  openGraph: {
    type: "website",
    siteName: "Dev-Moya",
    url: SITE_URL,
    locale: "ko_KR",
    images: [{ url: "/og-default.png", width: 1200, height: 630 }],
  },
  twitter: {
    card: "summary_large_image",
    title: "Dev-Moya",
    description: "개발 블로그 — 기술 지식을 공유합니다.",
    images: ["/og-default.png"],
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className={`${sans.variable} ${mono.variable}`}>
      <body>
        <Providers>{children}</Providers>
        <JsonLd data={buildOrganizationSchema(SITE_URL)} />
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
