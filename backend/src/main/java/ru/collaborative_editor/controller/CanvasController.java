package ru.collaborative_editor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import ru.collaborative_editor.model.Cell;
import ru.collaborative_editor.model.UpdatedCells;
import ru.collaborative_editor.service.SendUpdates;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CanvasController {
    private final SendUpdates sendUpdates;
    private final Map<String, Set<String>> pixelCache = new ConcurrentHashMap<>();

    @MessageMapping("/draw/{canvasId}")
    public void handleDraw(
            @Payload UpdatedCells updatedCells,
            @DestinationVariable String canvasId,
            @Header("simpSessionId") String sessionId
    ) throws JsonProcessingException {
        List<Cell> filtered = updatedCells.value().stream()
                .filter(cell -> {
                    String key = cell.x() + ":" + cell.y() + ":" + cell.color();
                    return !pixelCache.computeIfAbsent(canvasId, k -> new HashSet<>()).contains(key);
                })
                .peek(cell -> pixelCache.get(canvasId).add(cell.x() + ":" + cell.y() + ":" + cell.color()))
                .collect(Collectors.toList());

        if (!filtered.isEmpty()) {
            sendUpdates.sendRealtimeUpdate(canvasId, new UpdatedCells(filtered), sessionId);

            List<Cell> canvasBuffer = sendUpdates.createOrGetCanvasById(canvasId);
            canvasBuffer.addAll(filtered);

            log.info("Added {} pixels to canvas {} buffer for database persistence", filtered.size(), canvasId);
        }
    }

}
