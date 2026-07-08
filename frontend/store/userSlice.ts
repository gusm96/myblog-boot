import { createSlice, PayloadAction } from "@reduxjs/toolkit";

interface UserState {
  isLoggedIn: boolean;
  accessToken: string | null;
}

const initialState: UserState = {
  isLoggedIn: false,
  accessToken: null,
};

const userSlice = createSlice({
  name: "user",
  initialState,
  reducers: {
    login: (state) => {
      state.isLoggedIn = true;
      state.accessToken = null;
    },
    logout: (state) => {
      state.isLoggedIn = false;
      state.accessToken = null;
    },
    updateAccessToken: (state) => {
      state.accessToken = null;
    },
  },
});

export const selectIsLoggedIn = (state: { user: UserState }) =>
  state.user.isLoggedIn;
export const selectAccessToken = (state: { user: UserState }) =>
  state.user.accessToken;

export const { login, logout, updateAccessToken } = userSlice.actions;

export default userSlice;
