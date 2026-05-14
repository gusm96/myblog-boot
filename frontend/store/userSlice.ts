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
    login: (state, action: PayloadAction<{ accessToken: string }>) => {
      state.isLoggedIn = true;
      state.accessToken = action.payload.accessToken;
    },
    logout: (state) => {
      state.isLoggedIn = false;
      state.accessToken = null;
    },
    updateAccessToken: (
      state,
      action: PayloadAction<{ accessToken: string }>
    ) => {
      state.accessToken = action.payload.accessToken;
    },
  },
});

export const selectIsLoggedIn = (state: { user: UserState }) =>
  state.user.isLoggedIn;
export const selectAccessToken = (state: { user: UserState }) =>
  state.user.accessToken;

export const { login, logout, updateAccessToken } = userSlice.actions;

export default userSlice;
