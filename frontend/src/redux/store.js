import { combineReducers, configureStore } from "@reduxjs/toolkit";
import logger from "redux-logger";
import storage from "redux-persist/lib/storage";
import persistReducer from "redux-persist/es/persistReducer";
import persistStore from "redux-persist/es/persistStore";
import userSlice from "./userSlice";

const rootReducer = combineReducers({
  user: userSlice.reducer,
});

const persistConfig = {
  key: "root",
  storage,
  whitelist: ["user"],
};

const persistedReducer = persistReducer(persistConfig, rootReducer);

const store = configureStore({
  reducer: persistedReducer,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      thunk: true,
      immutableCheck: true,
      serializableCheck: {
        ignoreActions: ["persist/PERSIST"],
      },
    }).concat(logger),
});

const persistor = persistStore(store);

export { store, persistor };
