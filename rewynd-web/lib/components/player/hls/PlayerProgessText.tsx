import { Typography } from "@mui/material";
import formatDuration from "format-duration";
import React from "react";

interface PlayerProgressTextProps {
  played: number;
  duration: number;
}

export function PlayerProgessText(props: PlayerProgressTextProps) {
  return (
    <Typography sx={{ lineHeight: 1 }}>
      {`${formatDuration(Math.floor(props.played) * 1000)} / ${formatDuration(
        Math.ceil(props.duration) * 1000,
      )}`}
    </Typography>
  );
}
