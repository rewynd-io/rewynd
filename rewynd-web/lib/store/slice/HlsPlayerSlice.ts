import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { MediaInfo } from "@rewynd.io/rewynd-client-typescript";
import { WebLog } from "../../log";

export interface MediaState {
  info?: MediaInfo;
  audioTrack?: string;
  videoTrack?: string;
  subtitleTrack?: string;
}

export interface HlsPlayerState {
  media: MediaState;
  duration: number;
  startOffset: number;
  played: number;
  available: number;
  controlsVisibleLast: number;
  loading: boolean;
}

const initialState: HlsPlayerState = {
  loading: false,
  media: {},
  controlsVisibleLast: 0,
  played: 0,
  available: 0,
  startOffset: 0,
  duration: 0,
};
const log = WebLog.getChildCategory("HlsPlayerSlice");
export const hlsPlayerSlice = createSlice({
  name: "hls",
  initialState: initialState,
  reducers: {
    setPlayed: (state, action: PayloadAction<number>) => {
      state.played = action.payload;
    },
    setMediaState: (state, action: PayloadAction<MediaState>) => {
      log.info("SetMediaState", action.payload);
      state.media = action.payload;
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    setAvailable: (state, action: PayloadAction<number>) => {
      state.available = action.payload;
    },
    setControlsVisibleLast: (state, action: PayloadAction<number>) => {
      state.controlsVisibleLast = action.payload;
    },
    setupMedia: (
      state,
      action: PayloadAction<{
        readonly duration: number;
        readonly startOffset: number;
      }>,
    ) => {
      log.info("SetupMedia", action.payload);
      state.duration = action.payload.duration;
      state.startOffset = action.payload.startOffset;
    },
  },
});
export const setupMedia = hlsPlayerSlice.actions.setupMedia;
export const setPlayed = hlsPlayerSlice.actions.setPlayed;
export const setLoading = hlsPlayerSlice.actions.setLoading;
export const setAvailable = hlsPlayerSlice.actions.setAvailable;
export const setControlsVisibleLast =
  hlsPlayerSlice.actions.setControlsVisibleLast;
export const setMediaState = hlsPlayerSlice.actions.setMediaState;
