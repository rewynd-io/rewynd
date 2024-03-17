import { Typography } from "@mui/material";
import React from "react";
import { MediaInfo } from "@rewynd.io/rewynd-client-typescript";
import { mediaInfoTitle } from "../../util";

interface PlayerMediaTitleProps {
  readonly visible: boolean;
  readonly mediaInfo?: MediaInfo;
}

export function PlayerMediaTitle(props: PlayerMediaTitleProps) {
  return (
    <Typography
      sx={{
        position: "fixed",
        variant: "h1",
        component: "h1",
        top: 0,
        right: 0,
        opacity: 1,
        backgroundColor: "#00000000",
        visibility: props.visible ? "visible" : "hidden",
        zIndex: 1,
        padding: "1em",
      }}
    >
      {props.mediaInfo ? mediaInfoTitle(props.mediaInfo) : ""}
    </Typography>
  );
}
