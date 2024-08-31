import { createSlice, PayloadAction } from "@reduxjs/toolkit";

export interface EpisodeState {
  readonly nextId?: string;
  readonly previousId?: string;
  readonly percent?: number;
}

const initialState: EpisodeState = {};
export const episodeSlice = createSlice({
  name: "episode",
  initialState: initialState,
  reducers: {
    setEpisodeState: (_, action: PayloadAction<EpisodeState>) => {
      return { ...action.payload };
    },
  },
});
export const setEpisodeState = episodeSlice.actions.setEpisodeState;
