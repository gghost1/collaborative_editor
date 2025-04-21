package ru.collaborative_editor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import ru.collaborative_editor.model.Cell;
import ru.collaborative_editor.model.UpdatedCells;
import ru.collaborative_editor.service.SendUpdates;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CanvasController {

    private final SendUpdates sendUpdates;

    //api/draw/{canvasId}
    @MessageMapping("/draw/{canvasId}")
    public void handleDraw(@Payload UpdatedCells updatedCells, @DestinationVariable String canvasId) throws JsonProcessingException {
        log.info("handleDraw: {}", updatedCells);
        // Send immediate update for real-time rendering
        sendUpdates.sendRealtimeUpdate(canvasId, updatedCells);
        // Add new cells to the buffer
        List<Cell> cells = sendUpdates.createOrGetCanvasById(canvasId);
        cells.addAll(updatedCells.value());
    }

}
