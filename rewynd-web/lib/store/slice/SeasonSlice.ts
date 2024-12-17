import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { Thunk } from "../store";
import { HttpClient } from "../../const";
import {
  SerializableEpisodeInfo,
  SerializableSeasonInfo,
  toSerializableEpisodeInfo,
  toSerializableSeasonInfo,
} from "../../models";
import {
  ListEpisodesResponse,
  SeasonInfo,
} from "@rewynd.io/rewynd-client-typescript";

export interface SeasonEpisodesState {
  episodes: SerializableEpisodeInfo[];
  cursor?: string;
}

export interface SeasonSliceState {
  readonly season?: SerializableSeasonInfo;
  readonly episodes?: SeasonEpisodesState;
}

const initialState: SeasonSliceState = {};
export const seasonSlice = createSlice({
  name: "season",
  initialState: initialState,
  reducers: {
    setSeason: (
      _,
      action: PayloadAction<SeasonInfo | undefined>,
    ): SeasonSliceState => {
      return {
        season: action.payload
          ? toSerializableSeasonInfo(action.payload)
          : undefined,
      };
    },
    clearEpisodes: (state): SeasonSliceState => {
      return {
        ...state,
        episodes: undefined,
      };
    },
    appendEpisodes: (
      state,
      action: PayloadAction<ListEpisodesResponse>,
    ): SeasonSliceState => {
      return {
        ...state,
        episodes: {
          episodes: [
            ...(state.episodes?.episodes ?? []),
            ...action.payload.page.map(toSerializableEpisodeInfo),
          ],
          cursor: action.payload.cursor,
        },
      };
    },
  },
});
export const setSeason = seasonSlice.actions.setSeason;
export const appendEpisodes = seasonSlice.actions.appendEpisodes;
export const clearEpisodes = seasonSlice.actions.clearEpisodes;

export function fetchSeason(seasonId: string): Thunk {
  return async (dispatch) => {
    dispatch(setSeason(await HttpClient.getSeasons({ seasonId })));
    dispatch(clearEpisodes());
    dispatch(
      appendEpisodes(
        await HttpClient.listEpisodes({
          listEpisodesRequest: {
            seasonId,
          },
        }),
      ),
    );
  };
}

export function loadEpisodes(
  seasonId: string,
  cursor: string | undefined,
): Thunk {
  return async (dispatch) => {
    if (cursor && seasonId) {
      dispatch(
        appendEpisodes(
          await HttpClient.listEpisodes({
            listEpisodesRequest: {
              seasonId,
              cursor,
            },
          }),
        ),
      );
    }
  };
}
