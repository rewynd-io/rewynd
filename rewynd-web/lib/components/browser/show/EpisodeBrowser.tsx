import React from "react";
import { ButtonLink } from "../../ButtonLink";
import { useParams } from "react-router";
import { Typography } from "@mui/material";
import { EpisodeLoader } from "../../loader/show/EpisodeLoader";
import { WebRoutes } from "../../../routes";
import { NavBar } from "../../NavBar";
import { ApiImage } from "../../Image";

export function EpisodeBrowser() {
  const episodeId = useParams()["episodeId"];
  if (!episodeId) return <></>;

  return (
    <NavBar>
      <EpisodeLoader
        episodeId={episodeId}
        onLoad={(episode) => {
          return (
            <>
              <Typography>{episode.title}</Typography>
              <ButtonLink to={WebRoutes.Player.formatEpisodeRoute(episode.id)}>
                <ApiImage
                  id={episode.episodeImageId}
                  style={{ width: "100%" }}
                  alt={episode.title}
                ></ApiImage>{" "}
                <Typography align={"center"}>{episode.title}</Typography>
              </ButtonLink>
            </>
          );
        }}
      />
    </NavBar>
  );
}
