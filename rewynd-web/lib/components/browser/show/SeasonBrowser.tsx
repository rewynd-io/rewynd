import React from "react";

import { useParams } from "react-router";
import { EpisodesLoader } from "../../loader/show/EpisodesLoader";
import { Grid2 as Grid, Stack, Typography } from "@mui/material";
import { NavBar } from "../../NavBar";
import { List } from "immutable";
import { EpisodeCard } from "../card/EpisodeCard";
import { ApiImage } from "../../Image";
import { toSerializableEpisodeInfo } from "../../../models";

export function SeasonBrowser() {
  const season = useParams()["seasonId"];
  if (!season) return <></>;
  return (
    <NavBar>
      <EpisodesLoader
        seasonId={season}
        onLoad={({ episodeInfos, seasonInfo }) => {
          return (
            <Stack>
              <Stack direction={"row"}>
                <ApiImage
                  style={{ width: "30%" }}
                  alt={`${seasonInfo.showName} ${seasonInfo.seasonNumber} Season Image`}
                  id={seasonInfo.folderImageId}
                />
                <Stack direction={"column"}>
                  <Typography>{seasonInfo.showName}</Typography>
                  <Typography>Season {seasonInfo.seasonNumber}</Typography>
                  <Typography>
                    {seasonInfo.releaseDate?.toDateString()}
                  </Typography>
                </Stack>
              </Stack>
              <Grid
                container
                direction={"row"}
                sx={{ height: "100%" }}
                key={`EpisodesContainer-${season}`}
              >
                {List(episodeInfos)
                  .sortBy((it) => it.episode)
                  .map((showEpisodeInfo) => {
                    // TODO fetch progress to be displayed on the cards
                    return (
                      // <Box sx={{ minHeight: "10em" }} key={showEpisodeInfo.id}>
                      <EpisodeCard
                        episode={toSerializableEpisodeInfo(showEpisodeInfo)}
                        key={showEpisodeInfo.id}
                      />
                      // </Box>
                    );
                  })}
              </Grid>
            </Stack>
          );
        }}
      />
    </NavBar>
  );
}
