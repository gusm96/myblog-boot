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
export const { login, logout, updateAccessToken } = userSlice.actions;

export default userSlice.reducer;
