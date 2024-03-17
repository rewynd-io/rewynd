import { Navigate, useParams } from "react-router";
import React, { useEffect, useState } from "react";
import { Library } from "@rewynd.io/rewynd-client-typescript";
import { HttpClient } from "../../const";
import { ShowLibraryBrowser } from "./show/ShowLibraryBrowser";
import { CircularProgress, Grid } from "@mui/material";
import { WebRoutes } from "../../routes";
import { MovieLibraryBrowser } from "./movie/MovieLibraryBrowser";

export function LibraryBrowser() {
  const libraryId = useParams()["libraryId"];
  if (!libraryId) return <Navigate to={WebRoutes.root} />;

  const [library, setLibrary] = useState<Library>();
  useEffect(() => {
    HttpClient.getLibrary({ libraryId: libraryId }).then(setLibrary);
  }, [libraryId]);

  if (library) {
    switch (library.type) {
      case "Show":
        return <ShowLibraryBrowser />;
      case "Movie":
        return <MovieLibraryBrowser />;
      default:
        return <Navigate to={WebRoutes.root} />;
    }
  }
  return (
    <Grid
      container
      sx={{ height: "100vh", width: "100vw", overflow: "hidden" }}
      direction={"row"}
      alignItems={"center"}
      justifyContent={"center"}
    >
      <Grid
        item
        container
        direction={"column"}
        alignItems={"center"}
        justifyContent={"center"}
      >
        <CircularProgress />
      </Grid>
    </Grid>
  );
}
