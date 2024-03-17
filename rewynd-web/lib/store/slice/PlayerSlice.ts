import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { HlsPlayerManager } from "../../components/player/hls/HlsPlayerManager";
import { MediaSelection } from "../../components/player/hls/HlsPlayer";

export interface PlayerState {
  readonly hls: HlsPlayerManager;
  readonly startOffset: number;
  readonly duration: number;
}

const initialState: PlayerState = {
  hls: new HlsPlayerManager(),
  startOffset: 0,
  duration: 0,
};
export const playerSlice = createSlice({
  name: "player",
  initialState: initialState,
  reducers: {
    seek: (state, action: PayloadAction<number>) => {
      state.hls.seek(action.payload);
    },
    load: (state, action: PayloadAction<MediaSelection>) => {
      state.hls.load(action.payload);
    },
    unload: (state) => {
      state.hls.unload();
    },
    unbind: (state) => {
      state.hls.unbind();
    },
    bind: (
      // TODO fix state's type declaration
      state: { hls: { bind: (arg0: HTMLVideoElement) => void } },
      element: PayloadAction<HTMLVideoElement>,
    ) => {
      state.hls.bind(element.payload);
    },
  },
});

export const seek = playerSlice.actions.seek;
export const load = playerSlice.actions.load;
export const unload = playerSlice.actions.unload;
export const unbind = playerSlice.actions.unbind;
export const bind = playerSlice.actions.bind;
