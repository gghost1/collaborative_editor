import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import type { Pixel } from "../entities/Pixel";

type PixelsState = {
    history: Pixel[][]; // Полная история изменений
    pending: Pixel[];   // Пиксели для отправки
    rendered: Pixel[];  // Все отрендеренные пиксели
};

const initialState: PixelsState = {
    history: [],
    pending: [],
    rendered: []
};

export const pixelsSlice = createSlice({
    name: "pixels",
    initialState,
    reducers: {
        addPixels: (state, action: PayloadAction<Pixel[]>) => {
            state.pending.push(...action.payload);
            state.rendered.push(...action.payload);
        },
        flushPending: (state) => {
            if (state.pending.length > 0) {
                state.history.push(state.pending);
                state.pending = [];
            }
        },
        undo: (state) => {
            const lastBatch = state.history.pop();
            if (lastBatch) {
                state.rendered = state.rendered.filter(
                    p => !lastBatch.some(lp => lp.x === p.x && lp.y === p.y)
                );
            }
        },
        clear: (state) => {
            state.history = [];
            state.pending = [];
            state.rendered = [];
        }
    }
});

export const { addPixels, flushPending, undo, clear } = pixelsSlice.actions;
export default pixelsSlice.reducer;