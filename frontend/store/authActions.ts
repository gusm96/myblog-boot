import { login, logout } from "./userSlice";
import type { AppDispatch } from "./index";

export const userLogin = () => (dispatch: AppDispatch) => {
  dispatch(login());
};

export const userLogout = () => (dispatch: AppDispatch) => {
  dispatch(logout());
};

// 미사용 정리 (쿠키 연동으로 대체)
/*
export const updateUserAccessToken =
  (accessToken: string) => (dispatch: AppDispatch) => {
    dispatch(updateAccessToken());
  };
*/
