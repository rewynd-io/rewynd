import React, { useEffect, useRef } from "react";
import { PlayerSlider } from "./PlayerSlider";
import { Box, CircularProgress, Grid, Modal } from "@mui/material";
import { PlayerBottomBar } from "./PlayerBottomBar";
import { Duration } from "durr";
import {
  CreateStreamRequest,
  HlsStreamProps,
  MediaInfo,
} from "@rewynd.io/rewynd-client-typescript";
import { PlayerBackButton } from "../PlayerBackButton";
import { HlsPlayerManager } from "./HlsPlayerManager";
import {
  setAvailable,
  setControlsVisibleLast,
  setLoading,
  setMediaState,
  setPlayed,
  setupMedia,
} from "../../../store/slice/HlsPlayerSlice";
import { useAppDispatch, useAppSelector } from "../../../store/store";
import screenfull from "screenfull";
import { WebLog } from "../../../log";
import { PlayerMediaTitle } from "../PlayerMediaTitle";

export interface MediaSelection {
  request: CreateStreamRequest;
  info: MediaInfo;
}

export interface HlsPlayerProps {
  readonly onInit: () => Promise<MediaSelection | undefined>;
  readonly onComplete?: (
    lastRequest: CreateStreamRequest,
  ) => Promise<MediaSelection | undefined>;
  readonly onNext?: (
    lastRequest: CreateStreamRequest,
  ) => Promise<MediaSelection | undefined>;
  readonly onPrevious?: (
    lastRequest: CreateStreamRequest,
  ) => Promise<MediaSelection | undefined>;
  readonly onTracksChange?: (
    lastRequest: CreateStreamRequest,
    tracks: {
      audioTrackName?: string;
      videoTrackName?: string;
      subtitleTrackName?: string;
    },
  ) => Promise<MediaSelection | undefined>;
  readonly manager?: HlsPlayerManager;
}
const skipSeconds = 10;
const DEFAULT_MANAGER = new HlsPlayerManager();
const log = WebLog.getChildCategory("HlsPlayer");
export function HlsPlayer(props: HlsPlayerProps) {
  const manager = props.manager ?? DEFAULT_MANAGER;
  const dispatch = useAppDispatch();
  const loading = useAppSelector((state) => state.hls.loading);
  const media = useAppSelector((state) => state.hls.media);
  const played = useAppSelector((state) => state.hls.played);
  const duration = useAppSelector((state) => state.hls.duration);
  const available = useAppSelector((state) => state.hls.available);
  const startOffset = useAppSelector((state) => state.hls.startOffset);
  const controlsVisibleLast = useAppSelector(
    (state) => state.hls.controlsVisibleLast,
  );
  const setControlsVisible = () => dispatch(setControlsVisibleLast(Date.now()));
  const openControls =
    controlsVisibleLast >= Duration.seconds(3).before().getTime();
  const ref = useRef<HTMLVideoElement>(null);

  const setPaused = async (paused: boolean) => {
    log.info("setPaused", paused);
    if (!paused) {
      await ref.current?.play();
    } else {
      ref.current?.pause();
    }
  };

  const loadingOverlay = (pause = true) => {
    const paused = !!ref.current?.paused;
    if (pause) {
      ref?.current?.pause();
    }
    dispatch(setLoading(true));
    return paused;
  };

  const load = async (selection: MediaSelection, pause = true) => {
    const paused = loadingOverlay(pause);
    manager.once("load", () => {
      if (pause) {
        setPaused(paused);
      }
      dispatch(setLoading(false));
    });
    await manager.load(selection);
    dispatch(
      setMediaState({
        info: selection.info,
        audioTrack: selection.request.audioTrack,
        videoTrack: selection.request.videoTrack,
        subtitleTrack: selection.request.subtitleTrack,
      }),
    );
  };

  const seek = async (timestamp: number) => {
    const paused = loadingOverlay();
    manager.once("load", () => {
      setPaused(paused);
      dispatch(setLoading(false));
    });
    await manager.seek(timestamp);
  };

  const togglePlay = () => {
    if (ref.current?.paused) {
      ref.current?.play();
    } else {
      ref.current?.pause();
    }
  };

  useEffect(() => {
    if (props.onComplete) {
      const propOnComplete = props.onComplete;
      const onComplete = async (manager: HlsPlayerManager) => {
        const last = manager.lastMediaSelection?.request;
        if (last) {
          dispatch(setLoading(true));
          const next = await propOnComplete(last);
          if (next) {
            await load(next, false);
          }
        }
      };
      manager.on("next", onComplete);
      return () => {
        manager.off("next", onComplete);
      };
    } else return;
  }, [props.onComplete]);

  useEffect(() => {
    if (ref.current) {
      log.info("Binding to element");
      manager.bind(ref.current);
    }
    return () => {
      manager.unbind();
    };
  }, [ref.current]);

  useEffect(() => {
    const onLoad = (
      streamProps: HlsStreamProps,
      actualStartOffset: Duration,
    ) => {
      dispatch(
        setupMedia({
          duration: streamProps.duration,
          startOffset: actualStartOffset.seconds,
        }),
      );
      dispatch(setLoading(false));
      ref.current?.play().then();
    };
    manager.on("load", onLoad);

    (async () => {
      dispatch(setLoading(true));
      const selection = await props.onInit();
      if (selection) {
        log.info("Rendering new selection", selection);
        await load(selection, false);
      }
    })();

    const keyHandler = async (event: KeyboardEvent) => {
      event.stopPropagation();
      if (event.code === "Space") {
        togglePlay();
      } else if (event.code === "ArrowLeft") {
        await seek(Math.max(0, manager.currentTime - skipSeconds));
      } else if (event.code === "ArrowRight") {
        await seek(
          Math.min(manager.duration, manager.currentTime + skipSeconds),
        );
      } else if (event.code == "KeyF") {
        await screenfull.toggle();
      }
    };

    document.body.addEventListener("keyup", keyHandler);

    return () => {
      document.body.removeEventListener("keyup", keyHandler);
      manager.off("load", onLoad);
      manager.unload().then();
    };
  }, []);
  return (
    <Box
      sx={{ height: "100vh", width: "100vw", overflow: "hidden" }}
      onPointerMove={setControlsVisible}
      onTouchMove={setControlsVisible}
      onClick={() => {
        if (openControls) {
          togglePlay();
        } else {
          setControlsVisible();
        }
      }}
      onDoubleClick={() => screenfull.toggle().then()}
    >
      <Modal
        open={loading}
        sx={{ height: "100vh", width: "100vw", overflow: "hidden" }}
        onClick={(e) => e.stopPropagation()}
      >
        <Grid
          container
          sx={{ height: "100vh", width: "100vw", overflow: "hidden" }}
          direction={"row"}
          alignItems={"center"}
          justifyContent={"center"}
        >
          <Grid
            item
            container
            direction={"column"}
            alignItems={"center"}
            justifyContent={"center"}
          >
            <CircularProgress />
          </Grid>
        </Grid>
      </Modal>
      <PlayerBackButton
        visible={openControls}
        onBackButton={async () => {
          await manager.unload(true);
        }}
      />
      <PlayerMediaTitle
        visible={openControls}
        mediaInfo={manager.lastMediaSelection?.info}
      />
      <video
        ref={ref}
        onTimeUpdate={async () => {
          if (manager.available) {
            dispatch(setAvailable(manager.available));
          }
          if (ref.current) {
            dispatch(setPlayed(ref.current?.currentTime));
          }
        }}
        autoPlay={true}
        loop={false}
        style={{
          width: "100vw",
          height: "100vh",
          maxHeight: "100vh",
          maxWidth: "100vw",
        }}
        onError={(e) => log.error("Video Element encountered error", e)}
      />
      <PlayerBottomBar
        tracks={{
          // TODO add default user preferences, including default subtitle track and normlization
          videoTrackNames: Object.keys(media.info?.videoTracks ?? {}),
          audioTrackNames: Object.keys(media.info?.audioTracks ?? {}),
          subtitleTrackNames: Object.keys(media.info?.subtitleTracks ?? {}),
          selectedVideoTrackName: media.videoTrack,
          selectedAudioTrackName: media.audioTrack,
          selectedSubtitleTrackName: media.subtitleTrack,
          onAudioTrackChange: async (trackName) => {
            if (manager.lastMediaSelection) {
              const req = {
                ...manager.lastMediaSelection.request,
                startOffset: manager.currentTime,
                audioTrack: trackName,
              };
              await load({
                ...manager.lastMediaSelection,
                request: req,
              });
            }
          },
          onVideoTrackChange: async (trackName) => {
            if (manager.lastMediaSelection) {
              const req = {
                ...manager.lastMediaSelection.request,
                startOffset: manager.currentTime,
                videoTrack: trackName,
              };

              await load({
                ...manager.lastMediaSelection,
                request: req,
              });
            }
          },
          onSubtitleTrackChange: async (trackName) => {
            if (manager.lastMediaSelection) {
              const req = {
                ...manager.lastMediaSelection.request,
                startOffset: manager.currentTime,
                subtitleTrack: trackName,
              };

              await load({
                ...manager.lastMediaSelection,
                request: req,
              });
            }
          },
          onNormalizationMethodChange: async (method) => {
            if (manager.lastMediaSelection) {
              const req: CreateStreamRequest = {
                ...manager.lastMediaSelection.request,
                startOffset: manager.currentTime,
                normalization: method ? { method: method } : undefined,
              };
              console.log(req);

              await load({
                ...manager.lastMediaSelection,
                request: req,
              });
            }
          },
        }}
        openControls={openControls}
        playing={!(ref.current?.paused ?? true)}
        onPause={() => ref.current?.pause()}
        onPlay={() => ref.current?.play()}
        onNext={
          props.onNext
            ? async () => {
                if (props.onNext && manager.lastMediaSelection) {
                  const paused = loadingOverlay();
                  const next = await props.onNext(
                    manager.lastMediaSelection.request,
                  );
                  if (next) {
                    await load(next);
                  } else {
                    dispatch(setLoading(false));
                  }
                  await setPaused(paused);
                }
              }
            : undefined
        }
        onPrevious={
          props.onPrevious
            ? async () => {
                if (props.onPrevious && manager.lastMediaSelection) {
                  const paused = loadingOverlay();
                  const prev = await props.onPrevious(
                    manager.lastMediaSelection.request,
                  );
                  if (prev) {
                    await load(prev);
                  } else {
                    dispatch(setLoading(false));
                  }
                  await setPaused(paused);
                }
              }
            : undefined
        }
        onForward={async () => {
          await seek(Math.min(duration, manager.currentTime + skipSeconds));
        }}
        onBackward={async () => {
          await seek(Math.max(0, manager.currentTime - skipSeconds));
        }}
        slider={
          <PlayerSlider
            startOffset={startOffset}
            played={played}
            duration={duration}
            available={available}
            buffered={
              ref.current?.buffered?.length ?? 0 >= 1
                ? ref.current?.buffered?.end(
                    ref.current?.buffered?.length - 1,
                  ) ?? 0
                : 0
            }
            onSeek={async (desiredStreamTimestamp) => {
              await seek(desiredStreamTimestamp);
            }}
          />
        }
        played={played + (startOffset ?? 0)}
        duration={duration}
      />
    </Box>
  );
}
