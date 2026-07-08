/**
 * Axios 기반 클라이언트 사이드 API 클라이언트
 * 'use client' 컴포넌트 + TanStack Query에서 사용
 * - Authorization 헤더 자동 주입 (Redux store에서 토큰 읽음)
 * - withCredentials: true (refresh token 쿠키 전송)
 */

import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { store } from "@/store";
import { logout } from "@/store/userSlice";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

const apiClient = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
});

// 요청 인터셉터: 인증은 쿠키가 담당하므로 Bearer 헤더는 주입하지 않음
apiClient.interceptors.request.use((config) => {
  return config;
});

interface RetriedConfig extends InternalAxiosRequestConfig {
  _retried?: boolean;
}

// 응답 인터셉터: 401 시 access_token 쿠키 재발급 후 재요청
apiClient.interceptors.response.use(
  (res) => res,
  async (err: AxiosError) => {
    const config = err.config as RetriedConfig | undefined;
    if (err.response?.status !== 401 || !config || config._retried) {
      return Promise.reject(err);
    }
    config._retried = true;
    try {
      // refresh_token 쿠키를 이용해 access_token/refresh_token 재발급 (Set-Cookie로 쿠키 재설정됨)
      await axios.post(
        `${BASE_URL}/api/v1/reissuing-token`,
        {},
        { withCredentials: true }
      );
      // 재발급 성공 시 새 쿠키가 자동 첨부되므로 헤더 설정 없이 원요청 재시도
      return apiClient.request(config);
    } catch (e) {
      store.dispatch(logout());
      return Promise.reject(e);
    }
  }
);

export default apiClient;
