import React from "react";
import { ButtonLink } from "../../ButtonLink";
import { useParams } from "react-router";
import { Stack, Typography } from "@mui/material";
import { EpisodeLoader } from "../../loader/show/EpisodeLoader";
import { WebRoutes } from "../../../routes";
import { NavBar } from "../../NavBar";
import { ApiImage } from "../../Image";
import { UnpaddedButtonLink } from "../../styled/UnpaddedButtonLink";
import { formatEpisode, formatSeason } from "../../../util";
import formatEpisodeRoute = WebRoutes.formatEpisodeRoute;
import formatSeasonRoute = WebRoutes.formatSeasonRoute;

export function EpisodeBrowser() {
  const episodeId = useParams()["episodeId"];
  if (!episodeId) return <></>;

  return (
    <NavBar>
      <EpisodeLoader
        episodeId={episodeId}
        onLoad={(episode) => {
          const episodeRoute = formatEpisodeRoute(episode.id.toString());
          const seasonRoute = formatSeasonRoute(episode.seasonId.toString());
          return (
            <Stack direction={"row"}>
              <ButtonLink
                to={WebRoutes.Player.formatEpisodeRoute(episode.id)}
                style={{ width: "30%" }}
              >
                <ApiImage
                  id={episode.episodeImageId}
                  style={{ width: "100%" }}
                  alt={episode.title}
                ></ApiImage>{" "}
              </ButtonLink>

              <Stack direction={"column"}>
                <ButtonLink
                  to={WebRoutes.Player.formatEpisodeRoute(episode.id)}
                >
                  <Typography>{episode.title}</Typography>
                </ButtonLink>
                <Stack direction={"row"}>
                  <UnpaddedButtonLink to={seasonRoute}>
                    <Typography>{formatSeason(episode.season ?? 0)}</Typography>
                  </UnpaddedButtonLink>
                  <Typography>{":"}</Typography>
                  <UnpaddedButtonLink to={episodeRoute}>
                    <Typography>
                      {formatEpisode(
                        episode.episode ?? 0,
                        episode.episodeNumberEnd,
                      )}
                    </Typography>
                  </UnpaddedButtonLink>
                </Stack>
                <Typography>{episode.plot}</Typography>
                <Typography>{episode.outline}</Typography>
                <Typography>{episode.aired}</Typography>
                <Typography>Rating: {episode.rating}</Typography>
                {episode.credits?.map((actor) => (
                  <Typography key={actor}>{actor}</Typography>
                ))}
              </Stack>
            </Stack>
          );
        }}
      />
    </NavBar>
  );
}
