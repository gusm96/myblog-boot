/**
 * 인증 관련 API — 클라이언트 전용 (axios + 쿠키)
 */
import axios from "axios";
import apiClient from "./apiClient";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export const login = (formData: { username: string; password: string }) =>
  axios
    .post(`${BASE_URL}/api/v1/login`, formData, { withCredentials: true })
    .then((res) => res.data);

export const logout = () =>
  axios.get(`${BASE_URL}/api/v1/logout`, { withCredentials: true });

export const reissuingAccessToken = () =>
  axios
    .get(`${BASE_URL}/api/v1/reissuing-token`, { withCredentials: true })
    .then((res) => res.data);

export const validateAccessToken = () =>
  apiClient.get("/api/v1/token-validation").then((res) => res.data);

export const getRoleFromToken = () =>
  apiClient.get("/api/v1/token-role").then((res) => res.data);
