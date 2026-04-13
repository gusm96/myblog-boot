import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Docker standalone 빌드 (Node.js 서버 실행)
  output: "standalone",

  // 이미지 최적화 — 백엔드 업로드 이미지 허용
  images: {
    remotePatterns: [
      // 로컬 개발
      {
        protocol: "http",
        hostname: "localhost",
        port: "8080",
        pathname: "/api/v1/images/**",
      },
      // 운영: 도커 컴포즈 내부 통신 (backend 서비스명)
      {
        protocol: "http",
        hostname: "backend",
        port: "8080",
        pathname: "/api/v1/images/**",
      },
    ],
  },

  // 구 URL → 신 URL 301 리다이렉트
  async redirects() {
    return [
      // /boards → / (중복 라우트 제거)
      {
        source: "/boards",
        destination: "/",
        permanent: true,
      },
    ];
  },
};

export default nextConfig;
