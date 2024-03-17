import { createSlice, PayloadAction } from "@reduxjs/toolkit";

export const AdminSettingsTabs = ["users", "libraries", "schedules"] as const;
export type AdminSettingsTab = (typeof AdminSettingsTabs)[number];
export function getAdminSettingsTabName(tab: AdminSettingsTab): string {
  switch (tab) {
    case "libraries":
      return "Libraries";
    case "users":
      return "Users";
    case "schedules":
      return "Schedules";
  }
}

export interface AdminSettingsState {
  readonly tab: AdminSettingsTab;
}

const initialState: AdminSettingsState = {
  tab: "libraries",
};
export const adminSettingsSlice = createSlice({
  name: "adminSettings",
  initialState: initialState,
  reducers: {
    setTab: (state, element: PayloadAction<AdminSettingsTab>) => {
      state.tab = element.payload;
    },
  },
});

export const setTab = adminSettingsSlice.actions.setTab;
