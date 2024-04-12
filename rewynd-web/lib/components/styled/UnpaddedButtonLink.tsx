import { LinkProps } from "react-router-dom";
import { Link } from "../Link";
import React from "react";

export const UnpaddedButtonLink = (props: LinkProps) => (
  <Link
    style={{ padding: "0", fontSize: "1em" }}
    sx={{ padding: "0" }}
    {...props}
  />
);
