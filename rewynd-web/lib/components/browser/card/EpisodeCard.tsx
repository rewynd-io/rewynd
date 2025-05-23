import { Container, Stack, styled, Typography } from "@mui/material";
import { ButtonLink } from "../../ButtonLink";
import { WebRoutes } from "../../../routes";
import { ApiImage } from "../../Image";
import React, { PropsWithChildren } from "react";
import { cardWidth } from "../../../const";
import { formatEpisode, formatSeason } from "../../../util";
import { UnpaddedButtonLink } from "../../styled/UnpaddedButtonLink";
import formatShowRoute = WebRoutes.formatShowRoute;
import formatSeasonRoute = WebRoutes.formatSeasonRoute;
import formatEpisodeRoute = WebRoutes.formatEpisodeRoute;
import formatPlayerRoute = WebRoutes.Player.formatEpisodeRoute;
import { SerializableEpisodeInfo } from "../../../models";

const Text = styled(Typography)(({ theme }) => ({
  fontSize: "1em",
  display: "inline",
  padding: "0",
  color: theme.palette.primary.main,
}));

type RowProps = React.DetailedHTMLProps<
  React.HTMLAttributes<HTMLDivElement>,
  HTMLDivElement
> &
  PropsWithChildren;
const Row = (props: RowProps) => (
  <div
    {...props}
    style={{
      ...(props.style ?? {}),
      display: "flex",
      flexDirection: "row",
      justifyContent: "center",
    }}
  />
);

export interface EpisodeCardProps {
  readonly episode: SerializableEpisodeInfo;
}

export function EpisodeCard(props: EpisodeCardProps) {
  const episodeRoute = formatEpisodeRoute(props.episode.id.toString());
  const seasonRoute = formatSeasonRoute(props.episode.seasonId.toString());
  const showRoute = formatShowRoute(props.episode.showId.toString());
  const playerRoute = formatPlayerRoute(props.episode.id.toString());
  return (
    <Container sx={{ width: "auto", height: "15em", margin: "0" }}>
      <ButtonLink
        to={playerRoute}
        style={{ width: "100%" }}
        sx={{ width: "auto", height: "12em", minWidth: cardWidth }}
      >
        <Row style={{ width: "100%", height: "100%" }}>
          <ApiImage
            id={props.episode.episodeImageId}
            style={{ height: "100%" }}
            alt={props.episode.title}
          ></ApiImage>
        </Row>
      </ButtonLink>
      <Stack
        sx={{
          width: "100%",
          height: "25%",
          minHeight: "3em",
          minWidth: cardWidth,
        }}
      >
        <Row>
          <UnpaddedButtonLink to={showRoute}>
            <Text>{props.episode.showName}</Text>
          </UnpaddedButtonLink>
        </Row>
        <Row>
          <UnpaddedButtonLink to={seasonRoute}>
            <Text>{formatSeason(props.episode.season ?? 0)}</Text>
          </UnpaddedButtonLink>
          <Text>{":"}</Text>
          <UnpaddedButtonLink to={episodeRoute}>
            <Text>
              {formatEpisode(
                props.episode.episode ?? 0,
                props.episode.episodeNumberEnd,
              )}
            </Text>
          </UnpaddedButtonLink>
        </Row>
        <Row>
          <UnpaddedButtonLink to={episodeRoute}>
            <Text>{props.episode.title}</Text>
          </UnpaddedButtonLink>
        </Row>
      </Stack>
    </Container>
  );
}
