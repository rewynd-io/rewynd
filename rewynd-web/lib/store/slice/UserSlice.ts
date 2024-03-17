import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { User } from "@rewynd.io/rewynd-client-typescript";
import { useAppSelector } from "../store";

export interface UserState {
  readonly user?: User;
  readonly complete: boolean;
}

const initialState: UserState = {
  complete: false,
};
export const userSlice = createSlice({
  name: "user",
  initialState: initialState,
  reducers: {
    setUser: (state, element: PayloadAction<User | undefined>) => {
      state.user = element.payload;
    },
    setComplete: (state, element: PayloadAction<boolean>) => {
      state.complete = element.payload;
    },
  },
});

export const setUser = userSlice.actions.setUser;
export const setComplete = userSlice.actions.setComplete;

export const useUser = () => useAppSelector((state) => state.user.user);
