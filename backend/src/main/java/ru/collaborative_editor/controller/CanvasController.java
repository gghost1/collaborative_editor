package ru.collaborative_editor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import ru.collaborative_editor.model.Cell;
import ru.collaborative_editor.model.UpdateMessage;
import ru.collaborative_editor.model.UpdatedCells;
import ru.collaborative_editor.service.StoreUpdates;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CanvasController {

    private final StoreUpdates storeUpdates;

    //api/draw/{canvasId}
    @MessageMapping("/draw/{canvasId}")
    public void handleDraw(@Payload UpdatedCells updatedCells, @DestinationVariable String canvasId) throws JsonProcessingException {
        List<Cell> cells = storeUpdates.createOrGetCanvasById(canvasId);
        
        // Add new cells to the buffer
        cells.addAll(updatedCells.value());
    }

}
