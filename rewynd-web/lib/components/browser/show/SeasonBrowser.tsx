import React from "react";

import { useParams } from "react-router";
import { Grid, Stack, Typography } from "@mui/material";
import { NavBar } from "../../NavBar";
import { List } from "immutable";
import { EpisodeCard } from "../card/EpisodeCard";
import { ApiImage } from "../../Image";
import { useAppSelector, useThunkEffect } from "../../../store/store";
import { LoadingIcon } from "../../LoadingIcon";
import { fetchSeason, loadEpisodes } from "../../../store/slice/SeasonSlice";
import { compareEpisodeInfo } from "../../../util";

export function SeasonBrowser() {
  return (
    <NavBar>
      <InnerSeasonBrowser />
    </NavBar>
  );
}

function InnerSeasonBrowser() {
  const seasonId = useParams()["seasonId"];
  if (!seasonId) return <></>;

  const { episodes, season } = useAppSelector((state) => state.season);

  useThunkEffect(fetchSeason, seasonId);
  useThunkEffect(loadEpisodes, seasonId, episodes?.cursor);

  if (!episodes || !season) {
    return <LoadingIcon />;
  }
  return (
    <Stack>
      <Stack direction={"row"}>
        <ApiImage
          style={{ width: "30%" }}
          alt={`${season.showName} ${season.seasonNumber} Season Image`}
          id={season.folderImageId}
        />
        <Stack direction={"column"}>
          <Typography>{season.showName}</Typography>
          <Typography>Season {season.seasonNumber}</Typography>
          <Typography>{season.releaseDate}</Typography>
        </Stack>
      </Stack>
      <Grid
        container
        direction={"row"}
        sx={{ height: "100%" }}
        key={`EpisodesContainer-${season}`}
      >
        {List(episodes.episodes)
          .sort(compareEpisodeInfo)
          .map((showEpisodeInfo) => {
            // TODO fetch progress to be displayed on the cards
            return (
              <EpisodeCard episode={showEpisodeInfo} key={showEpisodeInfo.id} />
            );
          })}
      </Grid>
    </Stack>
  );
}
