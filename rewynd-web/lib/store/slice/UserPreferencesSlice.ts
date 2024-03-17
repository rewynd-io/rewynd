import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { UserPreferences } from "@rewynd.io/rewynd-client-typescript";

export const UserPreferencesTabs = ["user", "device", "admin"] as const;
export type UserPreferencesTab = (typeof UserPreferencesTabs)[number];
export function getUserPreferencesTabName(tab: UserPreferencesTab): string {
  switch (tab) {
    case "device":
      return "Device";
    case "user":
      return "User";
    case "admin":
      return "Admin";
  }
}

export interface UserPreferencesState {
  readonly draft?: UserPreferences;
}

const initialState: UserPreferencesState = {};
export const userPreferencesSlice = createSlice({
  name: "userPreferences",
  initialState: initialState,
  reducers: {
    setDraft: (state, element: PayloadAction<UserPreferences | undefined>) => {
      state.draft = element.payload;
    },
    setEnableSubtitlesByDefault: (state, element: PayloadAction<boolean>) => {
      if (state.draft) {
        state.draft.enableSubtitlesByDefault = element.payload;
      }
    },
  },
});

export const setDraft = userPreferencesSlice.actions.setDraft;

export const setEnableSubtitlesByDefault =
  userPreferencesSlice.actions.setEnableSubtitlesByDefault;
