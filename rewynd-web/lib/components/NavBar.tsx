import {
  AppBar,
  Autocomplete,
  autocompleteClasses,
  Box,
  Button,
  CircularProgress,
  Drawer,
  IconButton,
  TextField,
  Toolbar,
  Typography,
} from "@mui/material";
import React, {
  PropsWithChildren,
  SyntheticEvent,
  useRef,
  useState,
} from "react";
import MenuIcon from "@mui/icons-material/Menu";
import { Home, Logout, Settings, Search } from "@mui/icons-material";
import { RewyndIconTape } from "./RewyndIconTape";
import { ButtonLink } from "./ButtonLink";
import { WebRoutes } from "../routes";
import { HttpClient } from "../const";
import { SearchResult } from "@rewynd.io/rewynd-client-typescript";
import { useNavigate } from "react-router";
import formatEpisodeRoute = WebRoutes.formatEpisodeRoute;
import formatShowRoute = WebRoutes.formatShowRoute;
import formatSeasonRoute = WebRoutes.formatSeasonRoute;
import formatMovieRoute = WebRoutes.formatMovieRoute;

export type NavBarProps = PropsWithChildren;

export function NavBar(props: NavBarProps) {
  const [drawerOpen, setDrawerOpen] = useState(false);

  return (
    <Box
      sx={{
        width: "100vw",
        height: "100vh",
        overflowX: "hidden",
      }}
    >
      <AppBar>
        <Toolbar>
          <IconButton
            aria-label="menu"
            sx={{ mr: 2 }}
            onClick={() => setDrawerOpen(true)}
          >
            <MenuIcon />
          </IconButton>
          <ButtonLink to={WebRoutes.root}>
            <RewyndIconTape style={{ width: "4em", height: "auto" }} />
          </ButtonLink>
          <SearchInput />
        </Toolbar>
      </AppBar>
      <Toolbar />
      <Drawer
        anchor="left"
        sx={{ width: "25vw" }}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
      >
        <ButtonLink
          to={WebRoutes.root}
          onClick={() => setDrawerOpen(false)}
          style={{ justifyContent: "start" }}
        >
          <Home style={{ height: "1em" }} />
          <Typography style={{ marginLeft: "0.5em", height: "1em" }}>
            Home
          </Typography>
        </ButtonLink>
        <ButtonLink
          to={WebRoutes.settings}
          onClick={() => setDrawerOpen(false)}
          style={{ justifyContent: "start" }}
        >
          <Settings style={{ height: "1em" }} />
          <Typography style={{ marginLeft: "0.5em", height: "1em" }}>
            Settings
          </Typography>
        </ButtonLink>
        <Button
          onClick={() => {
            setDrawerOpen(false);
            HttpClient.logout().then(() => {
              window.location.reload();
            });
          }}
          style={{ justifyContent: "start" }}
        >
          <Logout style={{ height: "1em" }} />
          <Typography style={{ marginLeft: "0.5em", height: "1em" }}>
            Logout
          </Typography>
        </Button>
      </Drawer>
      {props.children}
    </Box>
  );
}

let callStack = Promise.resolve(); // make sure async calls for search occur in order
function SearchInput() {
  const ref = useRef<HTMLInputElement>();
  const nav = useNavigate();
  const [open, setOpen] = React.useState(false);
  const [inputValue, setInputValue] = React.useState("");
  const [options, setOptions] = React.useState<readonly SearchResult[]>([]);
  const loading = open && options.length === 0;
  console.log(loading);
  React.useEffect(() => {
    callStack = callStack.then(async () => {
      // only perform search if search text is currently displayed in the input
      console.log(ref.current?.value);
      if (ref.current?.value == inputValue) {
        setOptions([]);
        const res = await HttpClient.search({
          searchRequest: {
            text: inputValue,
          },
        });
        setOptions(res.results.sort((a, b) => b.score - a.score));
      }
    });
  }, [inputValue]);

  return (
    <Autocomplete
      id="search"
      sx={{
        width: 300,
        [`& .${autocompleteClasses.popupIndicator}`]: {
          transform: "none",
        },
      }}
      open={open}
      onOpen={async () => {
        setOpen(true);
      }}
      filterOptions={(x) => x}
      onClose={() => {
        setOpen(false);
      }}
      clearOnEscape={false}
      isOptionEqualToValue={(option, value) => option.title == value.title}
      includeInputInList={true}
      getOptionLabel={(option) => option.title}
      autoHighlight={true}
      autoSelect={false}
      clearOnBlur={false}
      // renderOption={(_: Object, option: SearchResult) => {
      //   return (
      //     <ButtonLink
      //       to={mkLink(option)}
      //       sx={{ width: "100%" }}
      //       style={{ width: "100%" }}
      //     >
      //       <Typography>{option.title}</Typography>
      //     </ButtonLink>
      //   );
      // }}
      popupIcon={<Search />}
      options={options}
      loading={loading}
      onChange={(_: SyntheticEvent, newValue: SearchResult | null) => {
        if (newValue) {
          nav(mkLink(newValue));
          setOpen(false);
        }
      }}
      onInputChange={(_, newInputValue) => {
        setInputValue(newInputValue);
      }}
      onKeyUp={(ev) => {
        console.log(`Pressed keyCode ${ev.key}`);
        if (ev.key === "Enter" && options[0]) {
          nav(mkLink(options[0]));
          ev.preventDefault();
        }
      }}
      renderInput={(params) => (
        <TextField
          {...params}
          label="Search"
          inputRef={ref}
          InputProps={{
            ...params.InputProps,
            endAdornment: (
              <React.Fragment>
                {loading ? (
                  <CircularProgress color="inherit" size={20} />
                ) : null}
                {params.InputProps.endAdornment}
              </React.Fragment>
            ),
          }}
        />
      )}
    />
  );
}

function mkLink(result: SearchResult) {
  switch (result.resultType) {
    case "Episode":
      return formatEpisodeRoute(result.id);
    case "Show":
      return formatShowRoute(result.id);
    case "Season":
      return formatSeasonRoute(result.id);
    case "Movie":
      return formatMovieRoute(result.id);
    default:
      return "";
  }
}
