import { login, logout, updateAccessToken } from "./userSlice";
import type { AppDispatch } from "./index";

export const userLogin = (accessToken: string) => (dispatch: AppDispatch) => {
  dispatch(login({ accessToken }));
};

export const userLogout = () => (dispatch: AppDispatch) => {
  dispatch(logout());
};

export const updateUserAccessToken =
  (accessToken: string) => (dispatch: AppDispatch) => {
    dispatch(updateAccessToken({ accessToken }));
  };
