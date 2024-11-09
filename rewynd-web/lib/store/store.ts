import { configureStore } from "@reduxjs/toolkit";
import { TypedUseSelectorHook, useDispatch, useSelector } from "react-redux";
import { episodeSlice } from "./slice/EpisodeSlice";
import { hlsPlayerSlice } from "./slice/HlsPlayerSlice";
import { userSlice } from "./slice/UserSlice";
import { settingsSlice } from "./slice/SettingsSlice";
import { userPreferencesSlice } from "./slice/UserPreferencesSlice";
import { adminSettingsSlice } from "./slice/AdminSettingsSlice";
import { useEffect } from "react";
import { homeSlice } from "./slice/HomeSlice";
import { seasonSlice } from "./slice/SeasonSlice";

export const store = configureStore({
  reducer: {
    // player: playerSlice.reducer,
    home: homeSlice.reducer,
    episode: episodeSlice.reducer,
    season: seasonSlice.reducer,
    hls: hlsPlayerSlice.reducer,
    user: userSlice.reducer,
    adminSettings: adminSettingsSlice.reducer,
    settings: settingsSlice.reducer,
    userPrefs: userPreferencesSlice.reducer,
  },
});

export type GetState = typeof store.getState;
// Infer the `RootState` and `AppDispatch` types from the store itself
export type RootState = ReturnType<GetState>;
// Inferred type: {posts: PostsState, comments: CommentsState, users: UsersState}
export type AppDispatch = typeof store.dispatch;

export type Thunk = (
  dispatch: AppDispatch,
  getState: GetState,
) => Promise<void>;

export function useThunkEffect<
  Params extends unknown[],
  ThunkGen extends (...args: Params) => Thunk,
>(thunkGen: ThunkGen, ...params: Params) {
  const dispatch = useAppDispatch();
  useEffect(() => {
    dispatch(thunkGen(...params));
  }, params);
}

// Use throughout your app instead of plain `useDispatch` and `useSelector`
export const useAppDispatch: () => AppDispatch = useDispatch;
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
