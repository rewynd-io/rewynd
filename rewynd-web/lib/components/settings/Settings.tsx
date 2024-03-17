import React, { PropsWithChildren } from "react";
import { Tab, Tabs } from "@mui/material";
import "../../declarations";
import { NavBar } from "../NavBar";
import { useUser } from "../../store/slice/UserSlice";
import { useAppDispatch, useAppSelector } from "../../store/store";
import {
  getSettingsTabName,
  setTab,
  SettingsTabs,
} from "../../store/slice/SettingsSlice";
import { AdminSettings } from "./AdminSettings";
import { UserSettings } from "./UserSettings";

export type SettingsProps = PropsWithChildren;

export function Settings() {
  const dispatch = useAppDispatch();
  const user = useUser();
  const tab = useAppSelector((state) => state.settings.tab);

  return (
    <NavBar>
      <Tabs value={tab} onChange={(_, val) => dispatch(setTab(val))}>
        {SettingsTabs.map((it) =>
          it !== "admin" || user?.permissions?.isAdmin ? (
            <Tab label={getSettingsTabName(it)} value={it} key={it} />
          ) : undefined,
        )}
      </Tabs>
      {tab === "user" ? (
        <UserSettings />
      ) : tab === "device" ? (
        <></>
      ) : tab === "admin" ? (
        <AdminSettings />
      ) : (
        <>Unknown Settings Tab </>
      )}
    </NavBar>
  );
}
