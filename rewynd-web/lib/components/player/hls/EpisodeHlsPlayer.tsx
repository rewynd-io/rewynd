import { Navigate, useNavigate, useParams } from "react-router";
import React from "react";
import { HlsPlayer, MediaSelection } from "./HlsPlayer";
import {
  useAppDispatch,
  useAppSelector,
  useThunkEffect,
} from "../../../store/store";
import { fetchEpisode } from "../../../store/slice/EpisodeSlice";
import { WebRoutes } from "../../../routes";
import { useUser } from "../../../store/slice/UserSlice";
import { LoadingIcon } from "../../LoadingIcon";
import { setCompleted } from "../../../store/slice/HlsPlayerSlice";

export function EpisodeHlsPlayer() {
  const { episodeId } = useParams();
  const nav = useNavigate();
  const user = useUser();
  const dispatch = useAppDispatch();
  if (!episodeId) {
    return <Navigate to={"/"} />;
  }

  const isComplete = useAppSelector((state) => state.hls.completed);

  useThunkEffect(fetchEpisode, episodeId);
  const state = useAppSelector((state) => state.episode.state);
  if (!state) {
    return <LoadingIcon />;
  }

  const { episode, next, previous } = state;

  const nextHandler = () => {
    dispatch(setCompleted(true));
    if (next) {
      nav(WebRoutes.Player.formatEpisodeRoute(next.id), { replace: true });
    }
  };
  return (
    <HlsPlayer
      onInit={async () => {
        dispatch(setCompleted(false));
        return {
          request: {
            audioTrack: Object.keys(episode.audioTracks)[0],
            videoTrack: Object.keys(episode.videoTracks)[0],
            subtitleTrack: user?.preferences?.enableSubtitlesByDefault
              ? Object.keys(episode.subtitleTracks)[0]
              : undefined,
            library: episode.libraryId,
            id: episode.id,
            startOffset: isComplete
              ? 0
              : episode.progress.percent * (episode.runTime ?? 0),
          },
          info: episode,
        } as MediaSelection;
      }}
      onNext={nextHandler}
      onPrevious={() => {
        dispatch(setCompleted(true));
        if (previous) {
          nav(WebRoutes.Player.formatEpisodeRoute(previous.id), {
            replace: true,
          });
        }
      }}
      onComplete={nextHandler}
    />
  );
}
