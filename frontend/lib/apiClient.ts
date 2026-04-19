/**
 * Axios 기반 클라이언트 사이드 API 클라이언트
 * 'use client' 컴포넌트 + TanStack Query에서 사용
 * - Authorization 헤더 자동 주입 (Redux store에서 토큰 읽음)
 * - withCredentials: true (refresh token 쿠키 전송)
 */

import axios from "axios";
import { store } from "@/store";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

const apiClient = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
});

// 요청 인터셉터: Redux store에서 직접 accessToken 읽어 Authorization 헤더 주입
apiClient.interceptors.request.use((config) => {
  const accessToken = store.getState().user.accessToken;
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

export default apiClient;
