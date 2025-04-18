import React, { PropsWithChildren, useState } from "react";
import { Box, Button, Grid, Menu, MenuItem } from "@mui/material";
import { PlayerProgessText } from "./PlayerProgessText";
import screenfull from "screenfull";
import {
  ArrowLeft,
  FastForward,
  FastRewind,
  Fullscreen,
  FullscreenExit,
  Pause,
  PlayArrow,
  SkipNext,
  SkipPrevious,
} from "@mui/icons-material";
import SettingsIcon from "@mui/icons-material/Settings";
import { NestedMenuItem } from "mui-nested-menu";
import { NormalizationMethod } from "@rewynd.io/rewynd-client-typescript";

interface Tracks {
  readonly audioTrackNames: string[];
  readonly videoTrackNames: string[];
  readonly subtitleTrackNames: string[];
  readonly selectedAudioTrackName?: string;
  readonly selectedVideoTrackName?: string;
  readonly selectedSubtitleTrackName?: string;
  readonly normalizationMethod?: NormalizationMethod;
  readonly onAudioTrackChange: (trackName?: string) => void;
  readonly onVideoTrackChange: (trackName?: string) => void;
  readonly onSubtitleTrackChange: (trackName?: string) => void;
  readonly onNormalizationMethodChange: (method?: NormalizationMethod) => void;
}

interface PlayerBottomBarProps {
  readonly openControls: boolean;
  readonly playing: boolean;
  readonly onPlay: () => void;
  readonly onPause: () => void;
  readonly onNext?: () => void;
  readonly onPrevious?: () => void;
  readonly onForward?: () => void;
  readonly onBackward?: () => void;
  readonly tracks: Tracks;
  readonly slider: React.JSX.Element;
  readonly played: number;
  readonly duration: number;
}

interface BarButtonProps extends PropsWithChildren {
  readonly onClick?: (e: React.MouseEvent<HTMLDivElement>) => void;
}

function BarButton(props: BarButtonProps) {
  return (
    <Grid
      container
      onClick={props.onClick}
      direction="column"
      justifyContent="center"
      alignItems="center"
      size={{ xs: "auto" }}
    >
      <Grid size={{ xs: "auto" }}>
        <Button disabled={!props.onClick}>{props.children}</Button>
      </Grid>
    </Grid>
  );
}

function PlayerSettings(props: PlayerBottomBarProps) {
  const [anchorEl, setAnchorEl] = useState<Element | undefined>(undefined);
  const open = !!anchorEl && props.openControls;

  const handleClose = () => setAnchorEl(undefined);

  return (
    <div onClick={(e) => e.stopPropagation()}>
      <BarButton
        onClick={(e) => {
          setAnchorEl(e.currentTarget as Element);
        }}
      >
        <SettingsIcon />
      </BarButton>
      <Menu
        anchorEl={anchorEl}
        open={open}
        onClose={handleClose}
        anchorOrigin={{ horizontal: "left", vertical: "top" }}
        transformOrigin={{
          vertical: "bottom",
          horizontal: "right",
        }}
      >
        <NestedMenuItem
          rightIcon={<></>}
          leftIcon={<ArrowLeft />}
          parentMenuOpen={open}
          label={"Video Track"}
          MenuProps={{
            anchorOrigin: {
              vertical: "center",
              horizontal: "left",
            },
            transformOrigin: {
              vertical: "bottom",
              horizontal: "right",
            },
          }}
        >
          {[undefined, ...props.tracks.videoTrackNames].map((it) => {
            return (
              <MenuItem
                key={it}
                selected={it != props.tracks.selectedVideoTrackName}
                onClick={() => {
                  if (it != props.tracks.selectedVideoTrackName) {
                    props.tracks.onVideoTrackChange(it);
                  }
                  handleClose();
                }}
              >
                {it ?? "Disabled"}
              </MenuItem>
            );
          })}
        </NestedMenuItem>
        <NestedMenuItem
          rightIcon={<></>}
          leftIcon={<ArrowLeft />}
          parentMenuOpen={open}
          label={"Audio Track"}
          MenuProps={{
            anchorOrigin: {
              vertical: "center",
              horizontal: "left",
            },
            transformOrigin: {
              vertical: "bottom",
              horizontal: "right",
            },
          }}
        >
          {[undefined, ...props.tracks.audioTrackNames].map((it) => {
            return (
              <MenuItem
                key={it}
                selected={it != props.tracks.selectedAudioTrackName}
                onClick={() => {
                  if (it != props.tracks.selectedAudioTrackName) {
                    props.tracks.onAudioTrackChange(it);
                  }
                  handleClose();
                }}
              >
                {it ?? "Disabled"}
              </MenuItem>
            );
          })}
        </NestedMenuItem>
        <NestedMenuItem
          rightIcon={<></>}
          leftIcon={<ArrowLeft />}
          parentMenuOpen={open}
          label={"Subtitle Track"}
          MenuProps={{
            anchorOrigin: {
              vertical: "center",
              horizontal: "left",
            },
            transformOrigin: {
              vertical: "bottom",
              horizontal: "right",
            },
          }}
        >
          {[undefined, ...props.tracks.subtitleTrackNames].map((it) => {
            return (
              <MenuItem
                key={it}
                selected={it != props.tracks.selectedSubtitleTrackName}
                onClick={() => {
                  if (it != props.tracks.selectedSubtitleTrackName) {
                    props.tracks.onSubtitleTrackChange(it);
                  }
                  handleClose();
                }}
              >
                {it ?? "Disabled"}
              </MenuItem>
            );
          })}
        </NestedMenuItem>
        <NestedMenuItem
          rightIcon={<></>}
          leftIcon={<ArrowLeft />}
          parentMenuOpen={open}
          label={"Audio Normalization"}
          MenuProps={{
            anchorOrigin: {
              vertical: "center",
              horizontal: "left",
            },
            transformOrigin: {
              vertical: "bottom",
              horizontal: "right",
            },
          }}
        >
          {(
            [undefined, ...Object.keys(NormalizationMethod)] as (
              | NormalizationMethodKey
              | undefined
            )[]
          ).map((it) => {
            return (
              <MenuItem
                key={it?.toString()}
                selected={
                  (it ? NormalizationMethod[it] : undefined) !=
                  props.tracks?.normalizationMethod
                }
                onClick={() => {
                  if (
                    (it ? NormalizationMethod[it] : undefined) !=
                    props.tracks.normalizationMethod
                  ) {
                    props.tracks.onNormalizationMethodChange(
                      it ? NormalizationMethod[it] : undefined,
                    );
                  }
                  handleClose();
                }}
              >
                {(it ?? "Disabled").toString()}
              </MenuItem>
            );
          })}
        </NestedMenuItem>
      </Menu>
    </div>
  );
}

export type NormalizationMethodKey = keyof typeof NormalizationMethod;

export function PlayerBottomBar(props: PlayerBottomBarProps) {
  return (
    <Box
      sx={{
        flexGrow: 1,
        position: "fixed",
        bottom: 0,
        opacity: 0.8,
        backgroundColor: "#000000",
        width: "100vw",
        visibility: props.openControls ? "visible" : "hidden",
        zIndex: 1,
      }}
    >
      <Grid container wrap="nowrap" justifyContent="center" alignItems="center">
        {props.onPrevious ? (
          <BarButton
            onClick={(e) => {
              e.stopPropagation();
              if (props.onPrevious) {
                props.onPrevious();
              }
            }}
          >
            <SkipPrevious />
          </BarButton>
        ) : (
          <></>
        )}
        <BarButton
          onClick={(e) => {
            e.stopPropagation();
            if (props.onBackward) {
              props.onBackward();
            }
          }}
        >
          <FastRewind />
        </BarButton>

        {props.playing ? (
          <BarButton
            onClick={(e) => {
              e.stopPropagation();
              props.onPause();
            }}
          >
            <Pause />
          </BarButton>
        ) : (
          <BarButton
            onClick={(e) => {
              e.stopPropagation();
              props.onPlay();
            }}
          >
            <PlayArrow />
          </BarButton>
        )}
        <BarButton
          onClick={(e) => {
            e.stopPropagation();
            if (props.onForward) {
              props.onForward();
            }
          }}
        >
          <FastForward />
        </BarButton>
        {props.onNext ? (
          <BarButton
            onClick={(e) => {
              e.stopPropagation();
              if (props.onNext) {
                props.onNext();
              }
            }}
          >
            <SkipNext />
          </BarButton>
        ) : (
          <></>
        )}
        <Grid
          container
          direction="column"
          justifyContent="center"
          alignItems="center"
          sx={{ width: "100%" }}
        >
          <Grid sx={{ width: "100%" }}>{props.slider}</Grid>
        </Grid>
        <BarButton>
          <PlayerProgessText played={props.played} duration={props.duration} />
        </BarButton>
        <PlayerSettings {...props} />
        {screenfull.isFullscreen ? (
          <BarButton
            onClick={(e) => {
              e.stopPropagation();
              screenfull.toggle().then();
            }}
          >
            <FullscreenExit />
          </BarButton>
        ) : (
          <BarButton
            onClick={(e) => {
              e.stopPropagation();
              screenfull.toggle().then();
            }}
          >
            <Fullscreen />
          </BarButton>
        )}
      </Grid>
    </Box>
  );
}
