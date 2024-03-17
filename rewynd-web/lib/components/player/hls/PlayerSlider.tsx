import React, { useEffect, useRef, useState } from "react";
import { Slider, Typography } from "@mui/material";
import "../../../static/css/StreamPlayer.css";
import formatDuration from "format-duration";
import { Duration } from "durr";

export interface PlayerSliderProps {
  startOffset?: number;
  played: number;
  duration: number;
  buffered?: number;
  available?: number;
  onSeek: (desiredSeconds: number) => Promise<void>;
}

export function PlayerSlider(props: PlayerSliderProps) {
  const sliderRef = useRef<HTMLDivElement>(null);
  const [isRaised, setIsRaised] = useState(false);
  const [isSeeking, setIsSeeking] = useState(false);
  const [value, setValue] = useState(props.played + (props.startOffset ?? 0));
  useEffect(() => {
    if (!isRaised && !isSeeking) {
      setValue(props.played + (props.startOffset ?? 0));
    }
  }, [props.played, props.startOffset, isRaised, isSeeking]);
  return (
    <div
      ref={sliderRef}
      style={{
        width: "100%",
      }}
    >
      <Slider
        sx={{
          width: "100%",
        }}
        valueLabelDisplay={"auto"}
        valueLabelFormat={(value) => {
          return (
            <Typography sx={{ lineHeight: 1 }}>
              {`${formatDuration(
                Duration.seconds(value).milliseconds,
              )} / ${formatDuration(
                Duration.seconds(Math.ceil(props.duration)).milliseconds,
              )}`}
            </Typography>
          );
        }}
        marks={[
          ...(props.buffered
            ? [{ value: props.buffered + (props.startOffset ?? 0) }]
            : []),
          ...(props.available
            ? [{ value: props.available + (props.startOffset ?? 0) }]
            : []),
          ...(props.startOffset ? [{ value: props.startOffset }] : []),
        ]}
        min={0}
        max={props.duration}
        value={value}
        onClick={(event) => {
          event.stopPropagation();
        }}
        onChange={(event, newVal) => {
          if (!isSeeking) {
            setIsRaised(true);
            const wanted = typeof newVal == "number" ? newVal : newVal[0];
            if (wanted) {
              setValue(wanted);
            }
          }
          event.stopPropagation();
        }}
        onChangeCommitted={(event, newVal) => {
          setIsRaised(false);
          const wanted = typeof newVal == "number" ? newVal : newVal[0];
          if (wanted) {
            setIsSeeking(true);
            setValue(wanted);
            props.onSeek(wanted).then(() => setIsSeeking(false));
          }
          event.stopPropagation();
        }}
      />
    </div>
  );
}
