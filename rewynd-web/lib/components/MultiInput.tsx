import { useEffect, useState } from "react";
import { Button, Container, TextField } from "@mui/material";
import { List } from "immutable";
import React from "react";
import { Add, Remove } from "@mui/icons-material";

export interface MultiInputProps {
  readonly value: List<string>;
  readonly onChange: (value: List<string>) => void;
}

export function MultiInput(props: MultiInputProps) {
  const [array, setArray] = useState<List<string>>(List(props.value));

  useEffect(() => {
    props.onChange(array);
  }, array.toArray());

  return (
    <>
      {array.map((value, index) => (
        <Container key={index}>
          <TextField
            value={value}
            onChange={(event) => {
              const newArr = array.set(index, event.target.value);
              setArray(newArr);
            }}
          />
          <Button
            onClick={() => {
              setArray(array.delete(index));
            }}
            disabled={array.size === 1}
          >
            <Remove />
          </Button>
        </Container>
      ))}
      <Button
        onClick={() => {
          setArray(array.push(""));
        }}
      >
        <Add />
      </Button>
    </>
  );
}
