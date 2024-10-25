import React, { useEffect } from "react";
import { ButtonLink } from "../../ButtonLink";
import { useParams } from "react-router";
import { Stack, Typography } from "@mui/material";
import { WebRoutes } from "../../../routes";
import { NavBar } from "../../NavBar";
import { ApiImage } from "../../Image";
import { UnpaddedButtonLink } from "../../styled/UnpaddedButtonLink";
import { formatEpisode, formatSeason } from "../../../util";
import { fetchEpisode } from "../../../store/slice/EpisodeSlice";
import { useAppDispatch, useAppSelector } from "../../../store/store";
import { LoadingIcon } from "../../LoadingIcon";
import { EpisodeCard } from "../card/EpisodeCard";
import formatSeasonRoute = WebRoutes.formatSeasonRoute;
import formatEpisodeRoute = WebRoutes.formatEpisodeRoute;

export function EpisodeBrowser() {
  return (
    <NavBar>
      <InnerEpisodeBrowser />
    </NavBar>
  );
}

function InnerEpisodeBrowser() {
  const episodeId = useParams()["episodeId"];
  if (!episodeId) return <></>;

  const dispatch = useAppDispatch();
  useEffect(() => {
    dispatch(fetchEpisode(episodeId));
  }, [episodeId]);

  const state = useAppSelector((state) => state.episode.state);
  if (!state) {
    return <LoadingIcon />;
  }
  const { episode, next, previous } = state;
  const episodeRoute = formatEpisodeRoute(episode.id.toString());
  const seasonRoute = formatSeasonRoute(episode.seasonId.toString());

  return (
    <Stack direction={"column"}>
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
          <ButtonLink to={WebRoutes.Player.formatEpisodeRoute(episode.id)}>
            <Typography>{episode.title}</Typography>
          </ButtonLink>
          <Stack direction={"row"}>
            <UnpaddedButtonLink to={seasonRoute}>
              <Typography>{formatSeason(episode.season ?? 0)}</Typography>
            </UnpaddedButtonLink>
            <Typography>{":"}</Typography>
            <UnpaddedButtonLink to={episodeRoute}>
              <Typography>
                {formatEpisode(episode.episode ?? 0, episode.episodeNumberEnd)}
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
      <Stack direction={"row"}>
        {previous ? <EpisodeCard episode={previous} /> : <></>}
        {next ? <EpisodeCard episode={next} /> : <></>}
      </Stack>
    </Stack>
  );
}
