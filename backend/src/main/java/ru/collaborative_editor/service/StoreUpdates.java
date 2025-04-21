package ru.collaborative_editor.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import ru.collaborative_editor.model.Cell;
import ru.collaborative_editor.model.UpdateMessage;
import ru.collaborative_editor.model.UpdatedCells;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoreUpdates {

    // key - canvasId, value - list of updates
    private final Map<String, List<Cell>> canvasUpdates = new ConcurrentHashMap<>();
    private final KafkaTemplate<String, String> socketKafkaTemplate;



    @Scheduled(fixedRateString = "${update.interval}", timeUnit = TimeUnit.SECONDS)
    public void sendBufferedUpdates() {
        canvasUpdates.forEach((canvasId, cells) -> {
            if (!cells.isEmpty()) {
                try {
                    UpdateMessage updateMessage = new UpdateMessage(
                        canvasId, 
                        new UpdatedCells(new ArrayList<>(cells))
                    );
                    socketKafkaTemplate.send("ws-draw", updateMessage.toJson());
                    cells.clear(); // Clear after sending
                } catch (JsonProcessingException e) {
                    // Handle error
                }
            }
        });
    }

    public List<Cell> createOrGetCanvasById(String canvasId) {
        return canvasUpdates.computeIfAbsent(canvasId, k -> 
            new CopyOnWriteArrayList<>());
    }

}
