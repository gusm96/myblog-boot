import axios from "axios";
import { BASE_URL } from "../apiConfig";

const apiClient = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
});

// 모듈 레벨 토큰 저장소 — App.js에서 setAuthToken()으로 갱신
let _token = null;

export const setAuthToken = (token) => {
  _token = token;
};

// 요청 인터셉터: Authorization 헤더 자동 주입
apiClient.interceptors.request.use((config) => {
  if (_token) {
    config.headers.Authorization = `bearer ${_token}`;
  }
  return config;
});

export default apiClient;
