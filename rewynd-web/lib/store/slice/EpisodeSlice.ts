import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { EpisodeInfo } from "@rewynd.io/rewynd-client-typescript";

export interface EpisodeState {
  readonly currentEpisode?: EpisodeInfo;
  readonly nextId?: string;
  readonly previousId?: string;
  readonly percent?: number;
}

const initialState: EpisodeState = {};
export const episodeSlice = createSlice({
  name: "episode",
  initialState: initialState,
  reducers: {
    reset: () => {
      return { ...initialState };
    },
    setEpisodeState: (_, action: PayloadAction<EpisodeState>) => {
      return { ...action.payload };
    },
    setNextId: (state, action: PayloadAction<string | undefined>) => {
      state.nextId = action.payload;
    },
    setPercent: (state, action: PayloadAction<number | undefined>) => {
      state.percent = action.payload;
    },
    setPreviousId: (state, action: PayloadAction<string | undefined>) => {
      state.previousId = action.payload;
    },
  },
});
export const reset = episodeSlice.actions.reset;
export const setEpisodeState = episodeSlice.actions.setEpisodeState;
export const setNextId = episodeSlice.actions.setNextId;
export const setPreviousId = episodeSlice.actions.setPreviousId;
