import React, { PropsWithChildren } from "react";
import { CssBaseline, ThemeProvider } from "@mui/material";
import { darkTheme } from "./Theme";
import { CookiesProvider } from "react-cookie";
import "../static/css/Root.css";
import { Provider } from "react-redux";
import { store } from "../store/store";

export function Root(props: PropsWithChildren) {
  return (
    // <React.StrictMode> // TODO Reenable StrictMode
    <Provider store={store}>
      <ThemeProvider theme={darkTheme}>
        <CssBaseline />
        <CookiesProvider>{props.children}</CookiesProvider>
      </ThemeProvider>
    </Provider>
    // </React.StrictMode>
  );
}
