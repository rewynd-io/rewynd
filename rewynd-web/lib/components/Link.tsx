import React, { PropsWithChildren } from "react";
import { Link as RouterLink, LinkProps } from "react-router-dom";
import { SxProps, Link as MuiLink } from "@mui/material";
export interface ButtonLinkProps extends LinkProps, PropsWithChildren {
  sx?: SxProps;
}

export function Link(props: ButtonLinkProps) {
  return (
    <MuiLink
      sx={props.sx}
      component={RouterLink}
      to={props.to}
      style={props.style}
    >
      {props.children}
    </MuiLink>
  );
}

export const InlineButtonLink = (props: ButtonLinkProps) => (
  <Link {...props} sx={{ ...(props?.sx ?? {}), display: "inline" }} />
);
