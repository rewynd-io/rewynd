import React, { useEffect, useState } from "react";
import { DataGrid, GridColDef } from "@mui/x-data-grid";
import {
  Button,
  ButtonGroup,
  Checkbox,
  Dialog,
  FormControlLabel,
  FormGroup,
  TextField,
  Typography,
} from "@mui/material";
import { HttpClient } from "../../../const";
import "../../../declarations";
import { UserPermissions } from "@rewynd.io/rewynd-client-typescript";
import { loadAllUsers } from "../../../util";

// TODO declare using value getter for type safety
const columns: GridColDef[] = [
  {
    field: "username",
    headerName: "Username",
  },
  {
    field: "permissions",
    headerName: "Admin",
    valueGetter: (value: UserPermissions) =>
      value?.isAdmin ? "true" : "false",
  },
];

interface DeleteUserDialogProps {
  open: boolean;
  onClose: () => void;
  selectedIds: string[];
}

function DeleteUserDialog(props: DeleteUserDialogProps) {
  return (
    <Dialog open={props.open} onClose={props.onClose}>
      <Typography color="red">{`Delete ${props.selectedIds.length} users?`}</Typography>
      <Button
        onClick={() =>
          HttpClient.deleteUsers({
            deleteUsersRequest: { users: props.selectedIds },
          }).then(() => props.onClose())
        }
      >
        Confirm
      </Button>
      <Button onClick={() => props.onClose()}>Cancel</Button>
    </Dialog>
  );
}

export function UserAdminSettings() {
  const [users, setUsers] = useState<Express.User[]>([]);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [createUserDialogOpen, setCreateUserDialogOpen] = useState(false);
  const [deleteUsersDialogOpen, setDeleteUsersDialogOpen] = useState(false);

  const fetchUsers = () => loadAllUsers().then(setUsers);

  useEffect(() => {
    fetchUsers();
  }, [createUserDialogOpen, deleteUsersDialogOpen]);

  return (
    <>
      <CreateUserDialog
        open={createUserDialogOpen}
        onComplete={async () => {
          setCreateUserDialogOpen(false);
          await fetchUsers();
        }}
      />
      <DeleteUserDialog
        open={deleteUsersDialogOpen}
        onClose={() => setDeleteUsersDialogOpen(false)}
        selectedIds={selectedIds}
      />
      <ButtonGroup>
        <Button onClick={() => setCreateUserDialogOpen(true)}>Create</Button>
        {/*<Button disabled={selectedIds.length != 1}>Modify</Button>*/}
        <Button
          disabled={selectedIds.length < 1}
          onClick={() => setDeleteUsersDialogOpen(true)}
        >
          Delete
        </Button>
      </ButtonGroup>
      <DataGrid
        getRowId={(row) => row.username}
        rows={users}
        columns={columns}
        pageSizeOptions={[100]}
        checkboxSelection
        onRowSelectionModelChange={(ids) => {
          setSelectedIds(Array.from(ids.ids).map((it) => it.toString()));
        }}
      ></DataGrid>
    </>
  );
}

interface CreateUserDialogProps {
  open: boolean;
  onComplete: () => void;
}

function validate(
  username: string,
  password: string,
  verifyPassword: string,
): boolean {
  return (
    password == verifyPassword && password.length > 8 && username.length > 1
  );
}

// TODO proper form validation
function CreateUserDialog(props: CreateUserDialogProps) {
  const [error, setError] = useState<string>();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [verifyPassword, setVerifyPassword] = useState("");
  const [isAdmin, setIsAdmin] = useState(false);
  const [complete, setComplete] = useState(false);

  useEffect(() => {
    setError(undefined);
    setUsername("");
    setPassword("");
    setVerifyPassword("");
    setIsAdmin(false);
    if (complete) {
      setComplete(false);
      props.onComplete();
    }
  }, [complete, props.open]);

  const valid = validate(username, password, verifyPassword);

  return (
    <Dialog open={props.open} onClose={() => setComplete(true)}>
      <FormGroup>
        {error ? <Typography color="red">{error}</Typography> : <></>}
        <FormControlLabel
          control={
            <TextField onChange={(it) => setUsername(it.target.value)} />
          }
          label="Username"
        />
        <FormControlLabel
          control={
            <TextField onChange={(it) => setPassword(it.target.value)} />
          }
          label="Password"
        />
        <FormControlLabel
          control={
            <TextField onChange={(it) => setVerifyPassword(it.target.value)} />
          }
          label="Verify Password"
        />
        <FormControlLabel
          control={
            <Checkbox
              checked={isAdmin}
              onChange={(it) => setIsAdmin(it.target.checked)}
            />
          }
          label="Admin"
        />
        <Button
          disabled={!valid}
          onClick={() =>
            HttpClient.createUser({
              createUserRequest: {
                username: username,
                permissions: {
                  isAdmin: isAdmin,
                },
                password: password,
              },
            })
              .then(props.onComplete)
              .catch(setError)
          }
        >
          Submit
        </Button>
        <Button onClick={() => setComplete(true)}>Close</Button>
      </FormGroup>
    </Dialog>
  );
}
