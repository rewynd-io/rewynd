import React, { useEffect, useState } from "react";
import {
  Button,
  Dialog,
  FormControlLabel,
  FormGroup,
  Stack,
  Switch,
  TextField,
  Typography,
} from "@mui/material";
import { HttpClient } from "../../const";
import { setComplete, setUser, useUser } from "../../store/slice/UserSlice";
import { useAppDispatch, useAppSelector } from "../../store/store";
import {
  setDraft,
  setEnableSubtitlesByDefault,
} from "../../store/slice/UserPreferencesSlice";
import deepEqual from "deep-equal";
import { WebLog } from "../../log";
const log = WebLog.getChildCategory("UserSettings");
export function UserSettings() {
  const [openChangePasswordDialog, setOpenChangePasswordDialog] =
    useState(false);
  const user = useUser();
  const draft = useAppSelector((state) => state.userPrefs.draft);
  const dispatch = useAppDispatch();
  const reloadUserPrefs = () => {
    dispatch(setUser(undefined));
    dispatch(setDraft(undefined));
    dispatch(setComplete(false));
  };

  useEffect(() => {
    if (user?.preferences && !draft) {
      dispatch(setDraft(user?.preferences));
    }
  }, [user?.preferences, draft]);

  return (
    <Stack>
      <Stack>
        <Typography>Actions</Typography>
        <Button onClick={() => setOpenChangePasswordDialog(true)}>
          Change Password
        </Button>
        <ChangePasswordDialog
          open={openChangePasswordDialog}
          onComplete={() => {
            setOpenChangePasswordDialog(false);
          }}
        />
      </Stack>
      <Stack>
        <Typography>Preferences</Typography>
        <FormControlLabel
          control={
            <Switch
              checked={draft?.enableSubtitlesByDefault}
              onChange={(_, checked) => {
                dispatch(setEnableSubtitlesByDefault(checked));
              }}
            />
          }
          label={<Typography>Enable subtitles by default</Typography>}
        />
        <Button
          disabled={deepEqual(user?.preferences, draft) && !!draft}
          onClick={() => {
            if (draft) {
              HttpClient.putUserPreferences({ userPreferences: draft })
                .catch((e) => log.error("Failed to put prefs", e))
                .then(reloadUserPrefs);
            }
          }}
        >
          Save
        </Button>
      </Stack>
    </Stack>
  );
}

interface ChangePasswordDialogProps {
  open: boolean;
  onComplete: () => void;
}

function validate(
  oldPassword: string,
  password: string,
  verifyPassword: string,
): boolean {
  return (
    password == verifyPassword && password.length > 8 && password != oldPassword
  );
}

// TODO proper form validation
function ChangePasswordDialog(props: ChangePasswordDialogProps) {
  const [error, setError] = useState<string>();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [verifyNewPassword, setVerifyNewPassword] = useState("");
  const [complete, setComplete] = useState(false);

  useEffect(() => {
    setError(undefined);
    setCurrentPassword("");
    setNewPassword("");
    setVerifyNewPassword("");
    if (complete) {
      setComplete(false);
      props.onComplete();
    }
  }, [complete, props.open]);

  const valid = validate(currentPassword, newPassword, verifyNewPassword);

  return (
    <Dialog open={props.open}>
      <FormGroup>
        {error ? <Typography color="red">{error}</Typography> : <></>}
        <FormControlLabel
          control={
            <TextField onChange={(it) => setCurrentPassword(it.target.value)} />
          }
          label="Current Password"
        />
        <FormControlLabel
          control={
            <TextField onChange={(it) => setNewPassword(it.target.value)} />
          }
          label="New Password"
        />
        <FormControlLabel
          control={
            <TextField
              onChange={(it) => setVerifyNewPassword(it.target.value)}
            />
          }
          label="Verify New Password"
        />
        <Button
          onClick={() => {
            setComplete(true);
          }}
        >
          Cancel
        </Button>
        <Button
          disabled={!valid}
          onClick={() => {
            HttpClient.changePassword({
              changePasswordRequest: {
                oldPassword: currentPassword,
                newPassword: newPassword,
              },
            })
              .then(() => {
                setComplete(true);
              })
              .catch(setError);
          }}
        >
          Submit
        </Button>
      </FormGroup>
    </Dialog>
  );
}
