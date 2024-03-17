import React from "react";
import ReactDOM from "react-dom/client";
import { Root } from "../components/Root";
import {
  createBrowserRouter,
  createRoutesFromElements,
  Outlet,
  Route,
  RouterProvider,
} from "react-router-dom";
import { Settings } from "../components/settings/Settings";
import { Login } from "../components/Login";
import { Auth } from "../components/Auth";
import { Home } from "../components/Home";
import { SeasonBrowser } from "../components/browser/show/SeasonBrowser";
import { ShowBrowser } from "../components/browser/show/ShowBrowser";
import { EpisodeBrowser } from "../components/browser/show/EpisodeBrowser";
import { WebRoutes } from "../routes";
import { EpisodeHlsPlayer } from "../components/player/hls/EpisodeHlsPlayer";
import { LibraryBrowser } from "../components/browser/LibraryBrowser";
import { MovieBrowser } from "../components/browser/movie/MovieBrowser";
import { MovieHlsPlayer } from "../components/player/hls/MovieHlsPlayer";

function Index() {
  const router = createBrowserRouter(
    createRoutesFromElements(
      <>
        <Route
          element={
            <Auth>
              <Outlet />
            </Auth>
          }
        >
          <Route path={WebRoutes.root}>
            <Route index element={<Home />} />
            <Route path={WebRoutes.library} element={<LibraryBrowser />} />
            <Route path={WebRoutes.show} element={<ShowBrowser />} />
            <Route path={WebRoutes.season} element={<SeasonBrowser />} />
            <Route path={WebRoutes.episode} element={<EpisodeBrowser />} />
            <Route path={WebRoutes.movie} element={<MovieBrowser />} />
            <Route path={WebRoutes.settings} element={<Settings />} />
          </Route>

          <Route
            path={WebRoutes.Player.episode}
            element={<EpisodeHlsPlayer />}
          />
          <Route path={WebRoutes.Player.movie} element={<MovieHlsPlayer />} />
        </Route>
        <Route path={WebRoutes.Auth.login} element={<Login />} />
      </>,
    ),
  );
  return (
    <Root>
      <RouterProvider router={router} />
    </Root>
  );
}

const domRoot = document.getElementById("root") as HTMLElement;
const root = ReactDOM.createRoot(domRoot);

root.render(<Index />);
