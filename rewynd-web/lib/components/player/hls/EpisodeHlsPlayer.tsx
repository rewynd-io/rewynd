import {
  CreateStreamRequest,
  EpisodeInfo,
  NextEpisodeOrder,
  Progress,
} from "@rewynd.io/rewynd-client-typescript";
import { Navigate, useNavigate, useParams } from "react-router";
import React, { useEffect } from "react";
import { HlsPlayer, MediaSelection } from "./HlsPlayer";
import { useAppDispatch, useAppSelector } from "../../../store/store";
import {
  EpisodeState,
  setEpisodeState,
  updateEpisodeState,
} from "../../../store/slice/EpisodeSlice";
import { HttpClient } from "../../../const";
import { WebRoutes } from "../../../routes";
import { useUser } from "../../../store/slice/UserSlice";
import { resetCompletedProgress } from "../../../util";
import formatEpisodeRoute = WebRoutes.Player.formatEpisodeRoute;

export function EpisodeHlsPlayer() {
  const { episodeId } = useParams();
  const nav = useNavigate();
  if (!episodeId) {
    return <Navigate to={"/"} />;
  }
  const dispatch = useAppDispatch();
  const nextId = useAppSelector((state) => state.episode.nextId);
  const prevId = useAppSelector((state) => state.episode.previousId);
  const user = useUser();
  useEffect(() => {
    const episodeMapping = getEpisodeMapping();
    console.log(
      "EpisodeId changed: " +
        episodeId +
        ", Mapping: " +
        JSON.stringify(episodeMapping),
    );

    if (episodeMapping) {
      if (episodeMapping.sourceEpisodeId == episodeId) {
        clearEpisodeMapping();
        nav(formatEpisodeRoute(episodeMapping.destEpisodeId), {
          replace: true,
        });
        return;
      } else {
        clearEpisodeMapping();
      }
    }
  }, [episodeId]);

  const nextHandler: (
    last: CreateStreamRequest,
  ) => Promise<MediaSelection | undefined> = async (
    last: CreateStreamRequest,
  ) => {
    console.log(`Handling next - ${JSON.stringify(last)}`);
    // don't depend on anything from react - it might not be up-to-date if not in focus
    const next = (
      await HttpClient.getNextEpisode({
        getNextEpisodeRequest: {
          episodeId: last.id,
        },
      })
    ).episodeInfo;
    if (next) {
      const [perc, nextNext]: [number | undefined, string | undefined] =
        await Promise.all([
          HttpClient.getUserProgress({ id: next.id }).then(
            (it: Progress) => resetCompletedProgress(it)?.percent,
          ),
          HttpClient.getNextEpisode({
            getNextEpisodeRequest: { episodeId: next.id },
          }).then((it) => it.episodeInfo.id),
        ]);
      saveEpisodeMapping(episodeId, next.id);

      // Update ui last
      dispatch(
        updateEpisodeState({
          percent: perc,
          nextId: nextNext,
          previousId: last.id,
          currentEpisode: next,
        }),
      );

      return {
        request: {
          audioTrack: Object.keys(next.audioTracks)[0],
          videoTrack: Object.keys(next.videoTracks)[0],
          subtitleTrack: Object.keys(next.subtitleTracks)[0],
          library: next.libraryId,
          id: next.id,
          startOffset: (perc ?? 0) * (next.runTime ?? 0),
        },
        info: next,
      };
    } else return undefined;
  };

  return (
    <HlsPlayer
      onInit={async () => {
        const [ep, prog, next, prev]: [
          EpisodeInfo,
          number | undefined,
          string | undefined,
          string | undefined,
        ] = await Promise.all([
          HttpClient.getEpisode({ episodeId: episodeId }),
          HttpClient.getUserProgress({ id: episodeId }).then(
            (it: Progress) => resetCompletedProgress(it)?.percent,
          ),
          HttpClient.getNextEpisode({
            getNextEpisodeRequest: { episodeId: episodeId },
          })
            .then((it) => it.episodeInfo.id)
            .catch(() => undefined),
          HttpClient.getNextEpisode({
            getNextEpisodeRequest: { episodeId: episodeId },
          })
            .then((it) => it.episodeInfo.id)
            .catch(() => undefined),
        ]);
        const epState: EpisodeState = {
          percent: prog,
          nextId: next,
          previousId: prev,
          currentEpisode: ep,
        };
        console.log("Setting episode :" + JSON.stringify(epState));
        dispatch(setEpisodeState(epState));
        return {
          request: {
            audioTrack: Object.keys(ep.audioTracks)[0],
            videoTrack: Object.keys(ep.videoTracks)[0],
            subtitleTrack: user?.preferences?.enableSubtitlesByDefault
              ? Object.keys(ep.subtitleTracks)[0]
              : undefined,
            library: ep.libraryId,
            id: ep.id,
            startOffset: (prog ?? 0) * (ep.runTime ?? 0),
          },
          info: ep,
        } as MediaSelection;
      }}
      onNext={nextId ? nextHandler : undefined}
      onPrevious={
        prevId
          ? async (last: CreateStreamRequest) => {
              const prev = (
                await HttpClient.getNextEpisode({
                  getNextEpisodeRequest: {
                    episodeId: last.id,
                    order: NextEpisodeOrder.Previous,
                  },
                })
              ).episodeInfo;
              if (prev) {
                const [perc, prevPrev]: [
                  number | undefined,
                  string | undefined,
                ] = await Promise.all([
                  HttpClient.getUserProgress({ id: prev.id }).then(
                    (it: Progress) => resetCompletedProgress(it)?.percent,
                  ),
                  HttpClient.getNextEpisode({
                    getNextEpisodeRequest: { episodeId: prev.id },
                  }).then((it) => it.episodeInfo.id),
                ]);
                saveEpisodeMapping(episodeId, prev.id);

                // Update ui last
                dispatch(
                  updateEpisodeState({
                    percent: perc,
                    nextId: last.id,
                    previousId: prevPrev,
                    currentEpisode: prev,
                  }),
                );

                return {
                  info: prev,
                  request: {
                    audioTrack: Object.keys(prev.audioTracks)[0],
                    videoTrack: Object.keys(prev.videoTracks)[0],
                    subtitleTrack: Object.keys(prev.subtitleTracks)[0],
                    library: prev.libraryId,
                    id: prev.id,
                    startOffset: (perc ?? 0) * (prev.runTime ?? 0),
                    // TODO fetch userProgress
                  },
                };
              } else {
                return undefined;
              }
            }
          : undefined
      }
      onComplete={nextHandler}
    />
  );
}

const episodeMappingItemName = "EpisodeMapping";
function clearEpisodeMapping() {
  localStorage.removeItem(episodeMappingItemName);
}

function saveEpisodeMapping(currentEpisodeId: string, nextEpisodeId: string) {
  const currMapping = getEpisodeMapping();
  if (currMapping?.destEpisodeId == currentEpisodeId) {
    setEpisodeMapping({
      sourceEpisodeId: currMapping.sourceEpisodeId,
      destEpisodeId: nextEpisodeId,
    });
  } else {
    setEpisodeMapping({
      sourceEpisodeId: currentEpisodeId,
      destEpisodeId: nextEpisodeId,
    });
  }
}

function setEpisodeMapping(mapping: EpisodeMapping) {
  localStorage.setItem(episodeMappingItemName, JSON.stringify(mapping));
}

function getEpisodeMapping(): EpisodeMapping | undefined {
  const item = localStorage.getItem(episodeMappingItemName);
  if (item) {
    return JSON.parse(item);
  } else {
    return undefined;
  }
}

export interface EpisodeMapping {
  readonly sourceEpisodeId: string;
  readonly destEpisodeId: string;
}
