import {
  EpisodeInfo,
  Library,
  SortOrder,
} from "@rewynd.io/rewynd-client-typescript";
import React, { useEffect, useState } from "react";
import { ButtonLink } from "./ButtonLink";
import { WebRoutes } from "../routes";
import { NavBar } from "./NavBar";
import { Grid2 as Grid, Stack, Typography } from "@mui/material";
import { List } from "immutable";
import { cardWidth, HttpClient } from "../const";
import { EpisodeCard } from "./browser/card/EpisodeCard";
import { WebLog } from "../log";
import "../util";
import { isNotNil, loadAllLibraries } from "../util";
import { ApiImage } from "./Image";
import { Link } from "./Link";
import { usePages } from "../Pagination";

const log = WebLog.getChildCategory("Home");

export function Home() {
  const [libraries, setLibraries] = useState<Library[]>([]);
  const [episodes, setEpisodes] = useState<EpisodeInfo[]>([]);
  const [nextEpisodes, setNextEpisodes] = useState<EpisodeInfo[]>([]);
  const [newestEpisodes] = usePages<EpisodeInfo, string>(
    async (cursor) => {
      const res = await HttpClient.listEpisodesByLastUpdated({
        listEpisodesByLastUpdatedRequest: {
          cursor: cursor,
          libraryIds: [],
          limit: 100,
        },
      });
      return [res.episodes, res.cursor];
    },
    undefined,
    100,
  );

  useEffect(() => {
    loadAllLibraries().then(setLibraries);
    HttpClient.listProgress({
      listProgressRequest: {
        minProgress: 0.05,
        maxProgress: 0.95,
        limit: 20,
      },
    })
      .then((it) => {
        const results = it.results;
        return Promise.all(
          results?.map(async (prog) => {
            try {
              return await HttpClient.getEpisode({ episodeId: prog.id });
            } catch (e) {
              log.error("Failed to load episode", e);
              return undefined;
            }
          }) ?? [],
        );
      })
      .then((it) =>
        setEpisodes(
          List(it)
            .filter(isNotNil)
            .sortBy((a) => a.progress.timestamp.getTime())
            .reverse()
            .toArray(),
        ),
      );

    HttpClient.listProgress({
      listProgressRequest: {
        minProgress: 0.95,
        maxProgress: 1,
        limit: 10,
      },
    })
      .then(async (it) => {
        const results = it.results;
        return (
          await Promise.all(
            results?.map(async (prog) => {
              try {
                const res = await HttpClient.getNextEpisode({
                  getNextEpisodeRequest: {
                    episodeId: prog.id,
                    sortOrder: SortOrder.Ascending,
                  },
                });
                if (res.episodeInfo) {
                  if (res.episodeInfo.progress.percent <= 0.05) {
                    return res.episodeInfo;
                  } else {
                    return undefined;
                  }
                } else {
                  return undefined;
                }
              } catch (e) {
                log.error("Failed to load episode", e);
                return undefined;
              }
            }) ?? [],
          )
        )?.filter((it) => isNotNil(it));
      })
      .then((it) =>
        setNextEpisodes(
          List(it)
            .filter(isNotNil)
            .sortBy((a) => a.progress.timestamp.getTime())
            .reverse()
            .toArray(),
        ),
      );
  }, []);

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
