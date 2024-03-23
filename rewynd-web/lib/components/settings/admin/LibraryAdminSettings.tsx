import React, { useEffect, useState } from "react";
import { DataGrid, GridColDef } from "@mui/x-data-grid";
import {
  Button,
  ButtonGroup,
  Dialog,
  FormControlLabel,
  FormGroup,
  MenuItem,
  Select,
  SelectChangeEvent,
  TextField,
  Typography,
} from "@mui/material";
import {
  Library,
  LibraryType as LibraryTypeObj,
} from "@rewynd.io/rewynd-client-typescript";
import type { LibraryType } from "@rewynd.io/rewynd-client-typescript";
import { List } from "immutable";
import { HttpClient } from "../../../const";
import { MultiInput } from "../../MultiInput";

const columns: GridColDef[] = [
  {
    field: "name",
    headerName: "Name",
  },
  {
    field: "type",
    headerName: "Type",
  },
  {
    field: "rootPaths",
    headerName: "Root Paths",
    valueGetter: (value) => JSON.stringify(value ?? []),
  },
];

interface DeleteLibraryDialogProps {
  open: boolean;
  onClose: () => void;
  selectedIds: string[];
}

function DeleteLibraryDialog(props: DeleteLibraryDialogProps) {
  return (
    <Dialog open={props.open} onClose={props.onClose}>
      <Typography color="red">{`Delete ${props.selectedIds.length} libraries?`}</Typography>
      <Button
        onClick={() =>
          HttpClient.deleteLibraries({
            deleteLibrariesRequest: { libraries: props.selectedIds },
          }).then(() => props.onClose())
        }
      >
        Confirm
      </Button>
      <Button onClick={() => props.onClose()}>Cancel</Button>
    </Dialog>
  );
}

interface ScanLibraryDialogProps {
  open: boolean;
  onClose: () => void;
  selectedIds: string[];
}

function ScanLibraryDialog(props: ScanLibraryDialogProps) {
  return (
    <Dialog open={props.open} onClose={props.onClose}>
      <Typography color="red">{`Scan ${props.selectedIds.length} libraries?`}</Typography>
      <Button
        onClick={() =>
          HttpClient.scanLibraries({
            scanLibrariesRequest: { libraryIds: props.selectedIds },
          }).then(() => props.onClose())
        }
      >
        Confirm
      </Button>
      <Button onClick={() => props.onClose()}>Cancel</Button>
    </Dialog>
  );
}

export function LibraryAdminSettings() {
  const [libraries, setLibraries] = useState<Library[]>([] as Library[]);
  const [selectedIds, setSelectedIds] = useState<string[]>([] as string[]);
  const [createLibraryDialogOpen, setCreateLibraryDialogOpen] = useState(false);
  const [scanLibraryDialogOpen, setScanLibraryDialogOpen] = useState(false);
  const [deleteLibrariesDialogOpen, setDeleteLibrariesDialogOpen] =
    useState(false);

  const fetchLibraries = () =>
    HttpClient.listLibraries().then((it) => setLibraries(it));

  useEffect(() => {
    fetchLibraries().then();
  }, [createLibraryDialogOpen, deleteLibrariesDialogOpen]);

  return (
    <>
      <CreateLibraryDialog
        libraries={libraries}
        open={createLibraryDialogOpen}
        onComplete={() => {
          setCreateLibraryDialogOpen(false);
          fetchLibraries().then();
        }}
      />
      <DeleteLibraryDialog
        open={deleteLibrariesDialogOpen}
        onClose={() => setDeleteLibrariesDialogOpen(false)}
        selectedIds={selectedIds}
      />
      <ScanLibraryDialog
        open={scanLibraryDialogOpen}
        onClose={() => setScanLibraryDialogOpen(false)}
        selectedIds={selectedIds}
      />
      <ButtonGroup>
        <Button onClick={() => setCreateLibraryDialogOpen(true)}>Create</Button>
        {/*<Button disabled={selectedIds.length != 1}>Modify</Button>*/}
        <Button
          disabled={selectedIds.length < 1}
          onClick={() => setDeleteLibrariesDialogOpen(true)}
        >
          Delete
        </Button>
        <Button
          disabled={selectedIds.length < 1}
          onClick={() => setScanLibraryDialogOpen(true)}
        >
          Scan
        </Button>
      </ButtonGroup>
      <DataGrid
        getRowId={(row: Library) => row.name}
        rows={libraries}
        columns={columns}
        pageSizeOptions={[100]}
        checkboxSelection
        onRowSelectionModelChange={(ids) => {
          setSelectedIds(ids.map((it) => it.toString()));
        }}
      ></DataGrid>
    </>
  );
}

interface CreateLibraryDialogProps {
  open: boolean;
  onComplete: () => void;
  libraries: Library[];
}

function validate(existingLibraries: Library[], name: string): boolean {
  return !existingLibraries.find((it) => it.name == name);
}

// TODO proper form validation
function CreateLibraryDialog(props: CreateLibraryDialogProps) {
  const [error, setError] = useState<string>();
  const [name, setName] = useState("");
  const [rootPaths, setRootPaths] = useState<List<string>>(List());
  const [type, setType] = useState<LibraryType>("Show");
  const [complete, setComplete] = useState(false);

  useEffect(() => {
    setError(undefined);
    setName("");
    setRootPaths(List());
    setType("Show");
    if (complete) {
      setComplete(false);
      props.onComplete();
    }
  }, [complete, props.open]);

  const valid = validate(props.libraries, name);
  const menuItems: JSX.Element[] = [];
  for (const type in LibraryTypeObj) {
    menuItems.push(<MenuItem value={type}>{type}</MenuItem>);
  }
  return (
    <Dialog open={props.open} onClose={() => setComplete(true)}>
      <FormGroup>
        {error ? <Typography color="red">{error}</Typography> : <></>}
        <FormControlLabel
          control={<TextField onChange={(it) => setName(it.target.value)} />}
          label="Name"
        />
        <FormControlLabel
          control={
            <MultiInput
              value={rootPaths}
              onChange={(it) => {
                setRootPaths(List(it));
              }}
            />
          }
          label="Root Paths"
        />
        <FormControlLabel
          control={
            <Select
              onChange={(it: SelectChangeEvent<string>) =>
                setType(it.target.value as LibraryType)
              }
              value={type}
            >
              {...menuItems}
            </Select>
          }
          label="Library Type"
        />
        <Button
          disabled={!valid}
          onClick={() =>
            HttpClient.createLibrary({
              library: {
                name: name,
                rootPaths: rootPaths.toArray(),
                type: type,
              },
            }).then(props.onComplete)
          }
        >
          Submit
        </Button>
        <Button onClick={() => setComplete(true)}>Close</Button>
      </FormGroup>
    </Dialog>
  );
}
