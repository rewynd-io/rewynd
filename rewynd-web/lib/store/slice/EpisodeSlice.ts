import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { Thunk } from "../store";
import { HttpClient } from "../../const";
import { SortOrder } from "@rewynd.io/rewynd-client-typescript";
import {
  SerializableEpisodeInfo,
  toSerializableEpisodeInfo,
} from "../../models";

export interface EpisodeState {
  readonly episode: SerializableEpisodeInfo;
  readonly next?: SerializableEpisodeInfo;
  readonly previous?: SerializableEpisodeInfo;
}

export interface EpisodeSliceState {
  state?: EpisodeState;
}

const initialState: EpisodeSliceState = {};
export const episodeSlice = createSlice({
  name: "episode",
  initialState: initialState,
  reducers: {
    setEpisodeState: (_, action: PayloadAction<EpisodeState | undefined>) => {
      return { state: action.payload };
    },
  },
});
export const setEpisodeState = episodeSlice.actions.setEpisodeState;

export function fetchEpisode(episodeId: string): Thunk {
  return async (dispatch) => {
    dispatch(setEpisodeState(undefined));
    const [episode, next, previous] = await Promise.all([
      HttpClient.getEpisode({ episodeId }),
      HttpClient.getNextEpisode({
        getNextEpisodeRequest: { episodeId, sortOrder: SortOrder.Ascending },
      }).then((it) => it.episodeInfo),
      HttpClient.getNextEpisode({
        getNextEpisodeRequest: { episodeId, sortOrder: SortOrder.Descending },
      }).then((it) => it.episodeInfo),
    ]);
    dispatch(
      setEpisodeState({
        episode: toSerializableEpisodeInfo(episode),
        next: next ? toSerializableEpisodeInfo(next) : undefined,
        previous: previous ? toSerializableEpisodeInfo(previous) : undefined,
      }),
    );
  };
}
