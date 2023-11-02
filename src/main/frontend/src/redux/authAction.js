import { login, logout, updateAccessToken } from "./userSlice";

// 로그인 액션
export const userLogin = (accessToken) => {
  return (dispatch) => {
    dispatch(login({ accessToken }));
  };
};

// 로그아웃 액션
export const userLogout = () => {
  return (dispatch) => {
    dispatch(logout());
  };
};

// 토큰 재발급 액션
export const updateUserAccessToken = (accessToken) => {
  return (dispatch) => {
    dispatch(updateAccessToken({ accessToken }));
  };
};
