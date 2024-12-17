import React, { useEffect } from "react";
import { ButtonLink } from "./ButtonLink";
import { WebRoutes } from "../routes";
import { NavBar } from "./NavBar";
import { Grid2 as Grid, Stack, Typography } from "@mui/material";
import { cardWidth } from "../const";
import { EpisodeCard } from "./browser/card/EpisodeCard";
import "../util";
import { ApiImage } from "./Image";
import { Link } from "./Link";
import { useAppDispatch, useAppSelector, useThunkEffect } from "../store/store";
import {
  clearLibraries,
  clearNewestEpisodes,
  clearNextEpisodes,
  clearStartedEpisodes,
  loadLibraries,
  loadNewestEpisodes,
  loadNextEpisodes,
  loadStartedEpisodes,
} from "../store/slice/HomeSlice";

export function Home() {
  const nextEpisodes = useAppSelector(
    (it) => it.home.nextEpisodesState?.episodes ?? [],
  );
  const episodes = useAppSelector(
    (it) => it.home.startedEpisodesState?.episodes ?? [],
  );
  const newestEpisodes = useAppSelector(
    (it) => it.home.newestEpisodesState?.episodes ?? [],
  );
  const libraries = useAppSelector(
    (it) => it.home.librariesState?.libraries ?? [],
  );

  const dispatch = useAppDispatch();
  useEffect(() => {
    dispatch(clearLibraries());
    dispatch(clearNewestEpisodes());
    dispatch(clearNextEpisodes());
    dispatch(clearStartedEpisodes());
  }, []);

  useThunkEffect(loadLibraries);
  useThunkEffect(loadNewestEpisodes);
  useThunkEffect(loadNextEpisodes);
  useThunkEffect(loadStartedEpisodes);

  const libEntries = libraries.map((lib) => {
    return { route: WebRoutes.formatLibraryRoute(lib.name), name: lib.name };
  });

  return (
    <NavBar>
      <Grid width={"100%"} container direction={"column"}>
        <Grid
          container
          direction={"row"}
          justifyContent={"flex-start"}
          wrap={"nowrap"}
          sx={{ overflowX: "scroll" }}
          width={"100%"}
        >
          <Typography>Libraries</Typography>
        </Grid>
        <Grid
          container
          direction={"row"}
          justifyContent={"flex-start"}
          wrap={"nowrap"}
          sx={{ overflowX: "scroll" }}
          height={"15em"}
        >
          {libEntries.map((libEntry) => {
            return (
              <Stack
                key={libEntry.name}
                sx={{ width: cardWidth }}
                direction={"column"}
              >
                <ButtonLink to={libEntry.route} sx={{ width: "100%" }}>
                  {/* TODO display random image from Library */}
                  <ApiImage alt={libEntry.name} sx={{ height: "12em" }} />
                </ButtonLink>
                <Link to={libEntry.route}>
                  <Typography align={"center"}>{libEntry.name}</Typography>
                </Link>
              </Stack>
            );
          })}
        </Grid>
        <Grid
          container
          direction={"row"}
          justifyContent={"flex-start"}
          wrap={"nowrap"}
          sx={{ overflowX: "scroll" }}
          width={"100%"}
        >
          <Typography>Continue Watching</Typography>
        </Grid>
        <Grid
          container
          direction={"row"}
          justifyContent={"flex-start"}
          wrap={"nowrap"}
          sx={{ overflowX: "scroll" }}
          minHeight={"15em"}
          width={"100%"}
        >
          {episodes.map((episode) => (
            <EpisodeCard episode={episode} key={episode.id} />
          ))}
        </Grid>
        <Grid
          container
          direction={"row"}
          justifyContent={"flex-start"}
          wrap={"nowrap"}
          sx={{ overflowX: "scroll" }}
          width={"100%"}
        >
          <Typography>Next Up</Typography>
        </Grid>
        <Grid
          container
          direction={"row"}
          justifyContent={"flex-start"}
          wrap={"nowrap"}
          sx={{ overflowX: "scroll" }}
          height={"15em"}
          width={"100%"}
        >
          {nextEpisodes.map((episode) => (
            <EpisodeCard episode={episode} key={episode.id} />
          ))}
        </Grid>
        <Grid
          container
          direction={"row"}
          justifyContent={"flex-start"}
          wrap={"nowrap"}
          sx={{ overflowX: "scroll" }}
          width={"100%"}
        >
          <Typography>New Additions</Typography>
        </Grid>
        <Grid
          container
          direction={"row"}
          justifyContent={"flex-start"}
          wrap={"nowrap"}
          sx={{ overflowX: "scroll" }}
          minHeight={"15em"}
          width={"100%"}
        >
          {newestEpisodes.map((episode) => (
            <EpisodeCard episode={episode} key={episode.id} />
          ))}
        </Grid>
      </Grid>
    </NavBar>
  );
}
