import { CircularProgress, Grid2 as Grid } from "@mui/material";
import React from "react";

export function LoadingIcon() {
  return (
    <Grid
      container
      direction="row"
      justifyContent="center"
      alignItems="center"
      sx={{
        width: "100%",
        height: "100%",
      }}
    >
      <Grid
        container
        direction="column"
        justifyContent="center"
        alignItems="center"
        sx={{
          width: "100%",
          height: "100%",
        }}
      >
        <CircularProgress />
      </Grid>
    </Grid>
  );
}
