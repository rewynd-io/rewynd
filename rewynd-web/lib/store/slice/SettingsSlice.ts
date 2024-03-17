import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { UserPreferences } from "@rewynd.io/rewynd-client-typescript";

export const SettingsTabs = ["user", "device", "admin"] as const;
export type SettingsTab = (typeof SettingsTabs)[number];
export function getSettingsTabName(tab: SettingsTab): string {
  switch (tab) {
    case "device":
      return "Device";
    case "user":
      return "User";
    case "admin":
      return "Admin";
  }
}

export interface SettingsState {
  readonly tab: SettingsTab;
  readonly draftUserPreferences?: UserPreferences;
}

const initialState: SettingsState = {
  tab: "user",
};
export const settingsSlice = createSlice({
  name: "settings",
  initialState: initialState,
  reducers: {
    setTab: (state, element: PayloadAction<SettingsTab>) => {
      state.tab = element.payload;
    },
  },
});

export const setTab = settingsSlice.actions.setTab;
