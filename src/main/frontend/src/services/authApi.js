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
    .post(
      `${MEMBER_LOGIN}`,
      {
        username: formData.username,
        password: formData.password,
      },
      {
        withCredentials: true,
      }
    )
    .then((res) => res.data);
};

export const logout = () => {
  return axios
    .get(`${MEMBER_LOGOUT}`, {
      withCredentials: true,
    })
    .catch((error) => console.log(error));
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

export const reissuingAccessToken = () => {
  return axios
    .get(`${REISSUING_TOKEN}`, { withCredentials: true })
    .then((res) => res.data);
};
