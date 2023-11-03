import axios from "axios";
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
    .post(`${MEMBER_LOGIN}`, {
      username: formData.username,
      password: formData.password,
    })
    .then((res) => res.data);
};

export const logout = (accessToken) => {
  return axios
    .get(`${MEMBER_LOGOUT}`, {
      headers: {
        Authorization: `bearer ${accessToken}`,
      },
    })
    .then((res) => res.data);
};

export const validateAccessToken = (accessToken) => {
  return axios
    .get(`${TOKEN_VALIDATION}`, {
      headers: {
        Authorization: `bearer ${accessToken}`,
      },
    })
    .then((res) => res.data);
};

export const getRoleFromToken = (accessToken) => {
  return axios
    .get(`${TOKEN_ROLE}`, {
      headers: {
        Authorization: `bearer ${accessToken}`,
      },
    })
    .then((res) => res.data);
};

export const reissuingAccessToken = (refreshTokenIdx) => {
  return axios
    .post(`${REISSUING_TOKEN}`, {
      refresh_token_dix: refreshTokenIdx,
    })
    .then((res) => res.data);
};
