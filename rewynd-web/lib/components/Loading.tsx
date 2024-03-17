import { ReactElement } from "react";
import { CircularProgress, Grid } from "@mui/material";
import React from "react";

export interface LoadingProps<T> {
  waitFor?: T;
  render?: (t: T) => ReactElement;
  onWait?: () => void;
  onWaitOnce?: () => void;
  onRender?: () => void;
}

export function Loading<T>(props: LoadingProps<T>): ReactElement {
  const [waitOnce, setWaitOnce] = React.useState(false);
  React.useEffect(() => {
    if (!props.waitFor && props.onWaitOnce && !waitOnce) {
      setWaitOnce(true);
      props.onWaitOnce();
    }
    if (props.waitFor == undefined && props.onWait) {
      props.onWait();
    }
    if (props.waitFor !== undefined && props.onRender) {
      props.onRender();
    }
  }, [props.waitFor]);

  if (props.waitFor && props.render) {
    return props.render(props.waitFor);
  } else {
    return (
      <Grid
        container
        direction="row"
        justifyContent="center"
        alignItems="center"
        sx={{
          width: "100%",
          height: "100%",
        }}
      >
        <Grid
          item
          container
          direction="column"
          justifyContent="center"
          alignItems="center"
          sx={{
            width: "100%",
            height: "100%",
          }}
        >
          <Grid item>
            <CircularProgress />
          </Grid>
        </Grid>
      </Grid>
    );
  }
}
