import React from "react";
import { useAppDispatch, useAppSelector } from "../../store/store";
import { NavBar } from "../NavBar";
import { Tab, Tabs } from "@mui/material";
import {
  setTab,
  AdminSettingsTabs,
  getAdminSettingsTabName,
} from "../../store/slice/AdminSettingsSlice";
import { UserAdminSettings } from "./admin/UserAdminSettings";
import { LibraryAdminSettings } from "./admin/LibraryAdminSettings";
import { ScheduleAdminSettings } from "./admin/ScheduleAdminSettings";
export function AdminSettings() {
  const dispatch = useAppDispatch();
  const tab = useAppSelector((state) => state.adminSettings.tab);

  return (
    <NavBar>
      <Tabs value={tab} onChange={(_, val) => dispatch(setTab(val))}>
        {AdminSettingsTabs.map((it) => (
          <Tab label={getAdminSettingsTabName(it)} value={it} key={it} />
        ))}
      </Tabs>
      {tab === "users" ? (
        <UserAdminSettings />
      ) : tab === "libraries" ? (
        <LibraryAdminSettings />
      ) : tab === "schedules" ? (
        <ScheduleAdminSettings />
      ) : (
        <>Unknown Settings Tab </>
      )}
    </NavBar>
  );
}
