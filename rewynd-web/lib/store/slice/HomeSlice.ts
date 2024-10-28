import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { Thunk } from "../store";
import { HttpClient } from "../../const";
import { Library } from "@rewynd.io/rewynd-client-typescript";
import {
  SerializableEpisodeInfo,
  toSerializableEpisodeInfo,
} from "../../models";
import { Page } from "../models";

export interface LibrariesState {
  libraries: Library[];
  cursor?: string;
}

export interface NewestEpisodesState {
  episodes: SerializableEpisodeInfo[];
  cursor?: string;
}

export interface NextEpisodesState {
  episodes: SerializableEpisodeInfo[];
  cursor?: string;
}

export interface StartedEpisodesState {
  episodes: SerializableEpisodeInfo[];
  cursor?: string;
}

export interface HomeSliceState {
  librariesState?: LibrariesState;
  newestEpisodesState?: NewestEpisodesState;
  nextEpisodesState?: NextEpisodesState;
  startedEpisodesState?: StartedEpisodesState;
}

const initialState: HomeSliceState = {};
export const homeSlice = createSlice({
  name: "home",
  initialState: initialState,
  reducers: {
    appendLibraries: (state, action: PayloadAction<Page<Library, string>>) => {
      state.librariesState = {
        libraries: [
          ...(state.librariesState?.libraries ?? []),
          ...action.payload.values,
        ],
        cursor: action.payload.cursor,
      };
    },
    clearLibraries: (state) => {
      state.librariesState = undefined;
    },

    appendNewestEpisodes: (
      state,
      action: PayloadAction<Page<SerializableEpisodeInfo, string>>,
    ) => {
      state.newestEpisodesState = {
        episodes: [
          ...(state.newestEpisodesState?.episodes ?? []),
          ...action.payload.values,
        ],
        cursor: action.payload.cursor,
      };
    },
    clearNewestEpisodes: (state) => {
      state.newestEpisodesState = undefined;
    },

    appendNextEpisodes: (
      state,
      action: PayloadAction<Page<SerializableEpisodeInfo, string>>,
    ) => {
      state.nextEpisodesState = {
        episodes: [
          ...(state.nextEpisodesState?.episodes ?? []),
          ...action.payload.values,
        ],
        cursor: action.payload.cursor,
      };
    },
    clearNextEpisodes: (state) => {
      state.nextEpisodesState = undefined;
    },

    appendStartedEpisodes: (
      state,
      action: PayloadAction<Page<SerializableEpisodeInfo, string>>,
    ) => {
      state.startedEpisodesState = {
        episodes: [
          ...(state.startedEpisodesState?.episodes ?? []),
          ...action.payload.values,
        ],
        cursor: action.payload.cursor,
      };
    },
    clearStartedEpisodes: (state) => {
      state.startedEpisodesState = undefined;
    },
  },
});

export const appendLibraries = homeSlice.actions.appendLibraries;
export const clearLibraries = homeSlice.actions.clearLibraries;
export const appendNewestEpisodes = homeSlice.actions.appendNewestEpisodes;
export const clearNewestEpisodes = homeSlice.actions.clearNewestEpisodes;
export const appendNextEpisodes = homeSlice.actions.appendNextEpisodes;
export const clearNextEpisodes = homeSlice.actions.clearNextEpisodes;
export const appendStartedEpisodes = homeSlice.actions.appendStartedEpisodes;
export const clearStartedEpisodes = homeSlice.actions.clearStartedEpisodes;

export function loadLibraries(): Thunk {
  return async (dispatch, getState) => {
    const state = getState().home.librariesState;
    if (!state || state.cursor) {
      const page = await HttpClient.listLibraries({
        listLibrariesRequest: { cursor: state?.cursor },
      });
      dispatch(appendLibraries({ cursor: page.cursor, values: page.page }));
    }
  };
}

export function loadNewestEpisodes(): Thunk {
  return async (dispatch, getState) => {
    const state = getState().home.newestEpisodesState;
    if (!state || state.cursor) {
      const response = await HttpClient.listEpisodes({
        listEpisodesRequest: {
          order: {
            property: "EpisodeAddedTimestamp",
            sortOrder: "Descending",
          },
          minProgress: 0,
          maxProgress: 0.95,
          cursor: state?.cursor,
        },
      });
      dispatch(
        appendNewestEpisodes({
          cursor: response.cursor,
          values: response.page.map(toSerializableEpisodeInfo),
        }),
      );
    }
  };
}

export function loadNextEpisodes(): Thunk {
  return async (dispatch, getState) => {
    const state = getState().home.nextEpisodesState;
    if (!state || state.cursor) {
      const page = await HttpClient.listNextEpisodes({
        listNextEpisodesRequest: {
          cursor: state?.cursor,
        },
      });
      dispatch(
        appendNextEpisodes({
          cursor: page.cursor,
          values: page.page.map(toSerializableEpisodeInfo),
        }),
      );
    }
  };
}

export function loadStartedEpisodes(): Thunk {
  return async (dispatch, getState) => {
    const state = getState().home.startedEpisodesState;
    if (!state || state.cursor) {
      const response = await HttpClient.listEpisodes({
        listEpisodesRequest: {
          order: {
            property: "ProgressTimestamp",
            sortOrder: "Descending",
          },
          minProgress: 0.05,
          maxProgress: 0.95,
          cursor: state?.cursor,
        },
      });
      dispatch(
        appendStartedEpisodes({
          cursor: response.cursor,
          values: response.page.map(toSerializableEpisodeInfo),
        }),
      );
    }
  };
}
