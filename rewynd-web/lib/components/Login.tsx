import { Button, FormGroup, Grid, TextField, Typography } from "@mui/material";
import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { HttpClient } from "../const";

export function Login() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string>();

  const nav = useNavigate();

  useEffect(() => {
    HttpClient.verify()
      .then(() => nav("/", { replace: true }))
      .catch(() => {});
  }, []);

  async function submit() {
    try {
      await HttpClient.login({
        loginRequest: {
          username: username,
          password: password,
        },
      });
      nav("/");
    } catch (e) {
      setError(JSON.stringify(e));
    }
    return false;
  }

  return (
    <Grid
      container
      direction="row"
      justifyContent="center"
      alignItems="center"
      sx={{
        width: "100%",
        height: "100vh",
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
          {error ? <Typography color="red">{error}</Typography> : <></>}
          <form
            action="lib/components/Login"
            method="post"
            onSubmit={(e) => {
              submit().then((result) => {
                if (result) {
                  nav("/", { replace: true });
                }
              });
              e.preventDefault();
              return false;
            }}
          >
            <FormGroup>
              <TextField
                id="username"
                name="username"
                type="text"
                autoComplete="username"
                helperText="Username"
                required
                onChange={(it) => setUsername(it.target.value)}
              />
              <TextField
                id="password"
                name="password"
                type="password"
                helperText="Password"
                autoComplete="password"
                required
                onChange={(it) => setPassword(it.target.value)}
              />
              <Button type="submit">Sign in</Button>
            </FormGroup>
          </form>
        </Grid>
      </Grid>
    </Grid>
  );
}
