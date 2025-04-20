import { configureStore } from "@reduxjs/toolkit";
import { pixelsSlice } from "./pixelsSlice"

export const store = configureStore({
  reducer: { pixels: pixelsSlice.reducer },
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch