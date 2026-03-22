import axios from "axios";
import apiClient from "./apiClient";
import {
  MEMBER_JOIN,
  MEMBER_LOGIN,
  MEMBER_LOGOUT,
  REISSUING_TOKEN,
  TOKEN_ROLE,
  TOKEN_VALIDATION,
} from "../apiConfig";

export const join = (formData) => {
  return axios
    .post(`${MEMBER_JOIN}`, {
      username: formData.username,
      password: formData.password1,
      nickname: formData.nickname,
    })
    .then((res) => res.data);
};

export const login = (formData) => {
  return axios
    .post(
      `${MEMBER_LOGIN}`,
      {
        username: formData.username,
        password: formData.password,
      },
      { withCredentials: true }
    )
    .then((res) => res.data);
};

export const logout = () => {
  return axios.get(`${MEMBER_LOGOUT}`, { withCredentials: true });
};

// 토큰 재발급은 인증 쿠키 기반 — 인터셉터 토큰 불필요
export const reissuingAccessToken = () => {
  return axios
    .get(`${REISSUING_TOKEN}`, { withCredentials: true })
    .then((res) => res.data);
};

export const validateAccessToken = () => {
  return apiClient.get(`${TOKEN_VALIDATION}`).then((res) => res.data);
};

export const getRoleFromToken = () => {
  return apiClient.get(`${TOKEN_ROLE}`).then((res) => res.data);
};
