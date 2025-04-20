import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import type { Pixel } from "../entities/Pixel";

type PixelsState = Pixel[][];

export const pixelsSlice = createSlice({
    name: "pixels",
    initialState: [] as PixelsState,
    reducers: {
        addPixels: (state, action: PayloadAction<Pixel[]>) => {
            state.push(action.payload);
        },
        undo(state) {
            state.pop();
        },
        clear(state) {
            state.length = 0;
        },
        setAll(state, action: PayloadAction<PixelsState>) {
            return action.payload;
        },
    },
});

export const { addPixels, undo, clear, setAll } = pixelsSlice.actions;
export default pixelsSlice.reducer;