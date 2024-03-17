import React, { PropsWithChildren, useEffect, useState } from "react";
import { HttpClient } from "../const";
import fallback from "../static/img/DefaultMediaIcon.svg";
import { WebLog } from "../log";
import { Box, BoxProps } from "@mui/material";

export interface ApiImageProps extends BoxProps, PropsWithChildren {
  readonly id?: string;
  readonly alt: string;
}

const log = WebLog.getChildCategory("ApiImage");

export function ApiImage(props: ApiImageProps) {
  const [src, setSrc] = useState<string>(fallback);

  useEffect(() => {
    if (props.id) {
      HttpClient.getImage({ imageId: props.id })
        .then(async (img) => {
          setSrc(URL.createObjectURL(img));
        })
        .catch((e) => {
          log.error("Failed to fetch Image", e);
        });
      return () => {
        if (src) {
          URL.revokeObjectURL(src);
        }
      };
    }
    return;
  }, [props.id]);

  return (
    <Box {...props}>
      <img
        alt={props.alt}
        src={src}
        style={{
          position: "absolute",
          left: "0",
          right: "0",
          top: "0",
          bottom: "0",
          margin: "auto",
          maxWidth: "100%",
          maxHeight: "100%",
        }}
      ></img>
      {props.children}
    </Box>
  );
}
