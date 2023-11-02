import axios from "axios";
import { MEMBER_LOGIN, TOKEN_ROLE } from "../apiConfig";

export const login = (formdata) => {
  return axios
    .post(`${MEMBER_LOGIN}`, {
      username: formdata.username,
      password: formdata.password,
    })
    .then((res) => res.data);
};

export const logout = () => {
  return;
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
