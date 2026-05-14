import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

/**
 * Proxy (Next.js 16 — 구 middleware) — /management/** 경로에 대한 1차 인증 게이트
 *
 * 백엔드가 로그인 시 HttpOnly + Secure 쿠키 `refresh_token`을 세팅한다.
 * - HTTPS(운영) 환경: refresh_token 쿠키 검사 → 없으면 /login 리다이렉트
 * - HTTP(개발) 환경: Secure 쿠키가 HTTP에서 전송되지 않으므로 프록시 검사 생략.
 *   실제 인증 보호는 management/layout.tsx의 클라이언트 역할(Role) 검증이 담당.
 */
export function proxy(request: NextRequest) {
  const isHttps = request.url.startsWith("https://");

  if (isHttps) {
    const refreshToken = request.cookies.get("refresh_token");
    if (!refreshToken) {
      const loginUrl = new URL("/login", request.url);
      loginUrl.searchParams.set("from", request.nextUrl.pathname);
      return NextResponse.redirect(loginUrl);
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/management/:path*"],
};
