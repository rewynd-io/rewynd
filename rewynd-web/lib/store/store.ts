import { configureStore } from "@reduxjs/toolkit";
import { TypedUseSelectorHook, useDispatch, useSelector } from "react-redux";
import { episodeSlice } from "./slice/EpisodeSlice";
import { hlsPlayerSlice } from "./slice/HlsPlayerSlice";
import { userSlice } from "./slice/UserSlice";
import { settingsSlice } from "./slice/SettingsSlice";
import { userPreferencesSlice } from "./slice/UserPreferencesSlice";
import { adminSettingsSlice } from "./slice/AdminSettingsSlice";

export const store = configureStore({
  reducer: {
    // player: playerSlice.reducer,
    episode: episodeSlice.reducer,
    hls: hlsPlayerSlice.reducer,
    user: userSlice.reducer,
    adminSettings: adminSettingsSlice.reducer,
    settings: settingsSlice.reducer,
    userPrefs: userPreferencesSlice.reducer,
  },
});

// Infer the `RootState` and `AppDispatch` types from the store itself
export type RootState = ReturnType<typeof store.getState>;
// Inferred type: {posts: PostsState, comments: CommentsState, users: UsersState}
export type AppDispatch = typeof store.dispatch;

// Use throughout your app instead of plain `useDispatch` and `useSelector`
export const useAppDispatch: () => AppDispatch = useDispatch;
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
