import React, { PropsWithChildren } from "react";
import { To, useNavigate } from "react-router";

export interface NavProps extends PropsWithChildren {
  readonly to: To | number;
}

export function Nav(props: NavProps) {
  const nav = useNavigate();
  if (typeof props.to == "number") {
    nav(props.to);
  } else {
    nav(props.to);
  }
  return <> {props.children} </>;
}
