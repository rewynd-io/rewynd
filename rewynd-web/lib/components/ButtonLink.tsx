import React, { PropsWithChildren } from "react";
import { Link, LinkProps } from "react-router-dom";
import { Button, SxProps } from "@mui/material";
export interface ButtonLinkProps extends LinkProps, PropsWithChildren {
  sx?: SxProps;
}

export function ButtonLink(props: ButtonLinkProps) {
  return (
    <Button sx={props.sx} component={Link} to={props.to} style={props.style}>
      {props.children}
    </Button>
  );
}

export const InlineButtonLink = (props: ButtonLinkProps) => (
  <ButtonLink {...props} sx={{ ...(props?.sx ?? {}), display: "inline" }} />
);
