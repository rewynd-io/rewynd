import React, { PropsWithChildren, useEffect } from "react";
import { Navigate } from "react-router";
import { CircularProgress } from "@mui/material";
import { WebRoutes } from "../routes";
import { HttpClient } from "../const";
import { WebLog } from "../log";
import { useAppDispatch, useAppSelector } from "../store/store";
import { setUser, setComplete } from "../store/slice/UserSlice";
type AuthProps = PropsWithChildren;

const log = WebLog.getChildCategory("Auth");
export function Auth(props: AuthProps) {
  const dispatch = useAppDispatch();
  const user = useAppSelector((state) => state.user.user);
  const complete = useAppSelector((state) => state.user.complete);

  useEffect(() => {
    if (!user) {
      dispatch(setComplete(false));
      HttpClient.verify()
        .then((res) => {
          dispatch(setUser(res));
          dispatch(setComplete(true));
        })
        .catch((e) => {
          log.error("Login failed", e);
          dispatch(setUser(undefined));
          dispatch(setComplete(true));
        });
    }
  }, [user, complete]);

  return user ? (
    <>{props.children}</>
  ) : complete ? (
    <Navigate to={WebRoutes.Auth.login} />
  ) : (
    <CircularProgress />
  );
}
