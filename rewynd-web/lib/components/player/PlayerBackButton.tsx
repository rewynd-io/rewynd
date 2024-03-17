import { Button } from "@mui/material";
import { ArrowBack } from "@mui/icons-material";
import React from "react";
import { useNavigate } from "react-router";
import { WebRoutes } from "../../routes";

interface PlayerBackButtonProps {
  readonly visible: boolean;
  readonly onBackButton?: () => void;
  readonly backButtonPath?: string;
}

export function PlayerBackButton(props: PlayerBackButtonProps) {
  const nav = useNavigate();
  return (
    <Button
      sx={{
        position: "fixed",
        top: 0,
        left: 0,
        opacity: 0.8,
        backgroundColor: "#000000",
        visibility: props.visible ? "visible" : "hidden",
        zIndex: 1,
      }}
      onClick={(e) => {
        if (props.backButtonPath) {
          nav(props.backButtonPath);
        } else if (history.length > 1) {
          nav(-1);
        } else {
          nav(WebRoutes.root);
        }

        if (props.onBackButton) {
          props.onBackButton();
        }
        e.stopPropagation();
      }}
    >
      <ArrowBack />
    </Button>
  );
}
