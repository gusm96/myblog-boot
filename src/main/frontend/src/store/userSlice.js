import { createSlice } from "@reduxjs/toolkit";

const userSlice = createSlice({
  name: "user",
  initialState: {
    isLoggedIn: false,
    accessToken: null,
    userType: null,
  },
  reducers: {
    login: (state, action) => {
      state.isLoggedIn = true;
      state.accessToken = action.payload.accessToken;
      state.userType = action.payload.userType;
    },
    logout: (state) => {
      state.isLoggedIn = false;
      state.accessToken = null;
      state.userType = null;
    },
    updateAccessToken: (state, action) => {
      state.accessToken = action.payload.accessToken;
    },
  },
});

export const selectIsLoggedIn = (state) => state.user.isLoggedIn;
export const selectAccessToken = (state) => state.user.accessToken;
export const selectUserType = (state) => state.user.userType;
export const { login, logout, updateAccessToken } = userSlice.actions;

export default userSlice;
