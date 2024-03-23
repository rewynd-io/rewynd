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
import { Library, Schedule } from "@rewynd.io/rewynd-client-typescript";
import { List } from "immutable";
import { HttpClient } from "../../../const";
import Cron from "react-cron-generator";
import { isResponseError } from "../../../util";
import { LibraryLoader } from "../../loader/LibraryLoader";

// TODO declare using value getter for type safety
const columns: GridColDef[] = [
  {
    field: "id",
    headerName: "Name",
  },
  {
    field: "cronExpression",
    headerName: "Cron Expression",
  },
  {
    field: "scanTasks",
    headerName: "Scan Tasks",
    valueGetter: (value) => JSON.stringify(value ?? []),
  },
];

interface DeleteScheduleDialogProps {
  open: boolean;
  onClose: () => void;
  selectedIds: string[];
}

function DeleteScheduleDialog(props: DeleteScheduleDialogProps) {
  return (
    <Dialog open={props.open} onClose={props.onClose}>
      <Typography color="red">{`Delete ${props.selectedIds.length} schedules?`}</Typography>
      <Button
        onClick={() =>
          HttpClient.deleteSchedule({
            deleteScheduleRequest: { ids: props.selectedIds },
          }).then(() => props.onClose())
        }
      >
        Confirm
      </Button>
      <Button onClick={() => props.onClose()}>Cancel</Button>
    </Dialog>
  );
}

export function ScheduleAdminSettings() {
  const [schedules, setSchedules] = useState<Schedule[]>([] as Schedule[]);
  const [libraries, setLibraries] = useState<Library[]>([] as Library[]);
  const [selectedIds, setSelectedIds] = useState<string[]>([] as string[]);
  const [createLibraryDialogOpen, setCreateLibraryDialogOpen] = useState(false);
  const [deleteLibrariesDialogOpen, setDeleteLibrariesDialogOpen] =
    useState(false);

  const updateLibrariesAndSchedules = () => {
    HttpClient.listLibraries().then((it) => setLibraries(it));
    HttpClient.listSchedules().then((it) => setSchedules(it));
  };

  useEffect(() => {
    updateLibrariesAndSchedules();
  }, [createLibraryDialogOpen, deleteLibrariesDialogOpen]);

  return (
    <>
      <CreateScheduleDialog
        libraries={libraries}
        schedules={schedules}
        open={createLibraryDialogOpen}
        onComplete={() => {
          setCreateLibraryDialogOpen(false);
          updateLibrariesAndSchedules();
        }}
      />
      <DeleteScheduleDialog
        open={deleteLibrariesDialogOpen}
        onClose={() => setDeleteLibrariesDialogOpen(false)}
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
      </ButtonGroup>
      <DataGrid
        getRowId={(row: Schedule) => row.id}
        rows={schedules}
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

interface CreateScheduleDialogProps {
  open: boolean;
  onComplete: () => void;
  libraries: Library[];
  schedules: Schedule[];
}

function validate(existingSchedules: Schedule[], name: string): boolean {
  return !existingSchedules.find((it) => it.id == name);
}

// TODO proper form validation
function CreateScheduleDialog(props: CreateScheduleDialogProps) {
  const [error, setError] = useState<string>();
  const [id, setId] = useState("");
  const [scanTasks, setScanTasks] = useState<List<string>>(List.of(""));
  const [cronExpression, setCronExpression] = useState<string>();
  const [complete, setComplete] = useState(false);

  useEffect(() => {
    setError(undefined);
    setId("");
    if (complete) {
      setComplete(false);
      props.onComplete();
    }
  }, [complete, props.open]);

  const handleChange = (event: SelectChangeEvent<string[]>) => {
    const {
      target: { value },
    } = event;
    setScanTasks(List(typeof value === "string" ? value.split(",") : value));
  };

  const valid = validate(props.schedules, id);
  return (
    <LibraryLoader
      onLoad={(libraries) => (
        <Dialog open={props.open} onClose={() => setComplete(true)}>
          <FormGroup>
            {error ? <Typography color="red">{error}</Typography> : <></>}
            <FormControlLabel
              control={<TextField onChange={(it) => setId(it.target.value)} />}
              label="Name"
            />
            <FormControlLabel
              control={
                <Select onChange={handleChange}>
                  {libraries.map((it) => {
                    return (
                      <MenuItem key={it.name} value={it.name}>
                        {it.name}
                      </MenuItem>
                    );
                  })}
                </Select>
              }
              label="Root Paths"
            />

            <Cron
              value={cronExpression}
              onChange={(val) => {
                setCronExpression(val);
              }}
              showResultText={true}
              showResultCron={true}
            />
            <Button
              disabled={!valid}
              onClick={() =>
                cronExpression &&
                HttpClient.createSchedule({
                  schedule: {
                    id: id,
                    cronExpression: cronExpression,
                    scanTasks: scanTasks
                      .map((it) => {
                        return { libraryId: it };
                      })
                      .toArray(),
                  },
                })
                  .catch((res) =>
                    setError(isResponseError(res) ? res.response.body : res),
                  )
                  .then(props.onComplete)
              }
            >
              Submit
            </Button>
            <Button onClick={() => setComplete(true)}>Close</Button>
          </FormGroup>
        </Dialog>
      )}
    />
  );
}
