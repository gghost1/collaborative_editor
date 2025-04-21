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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
@Service
@RequiredArgsConstructor
@Slf4j
public class SendUpdates {

    @Value("${spring.kafka.topic-socket}")
    private String topicSocket;

    @Value("${spring.kafka.topic-db}")
    private String topicDb;

    // key - canvasId, value - list of updates
    private final Map<String, List<Cell>> canvasUpdates = new ConcurrentHashMap<>();
    private final KafkaTemplate<String, String> socketKafkaTemplate;

    // Send immediate updates for real-time rendering
    public void sendRealtimeUpdate(String canvasId, UpdatedCells cells) throws JsonProcessingException {
        UpdateMessage updateMessage = new UpdateMessage(canvasId, cells);
        socketKafkaTemplate.send(topicSocket, updateMessage.toJson());
    }

    //TODO: implement batch processing for sending updates to db
    @Scheduled(fixedRateString = "${update.interval}", timeUnit = TimeUnit.SECONDS)
    public void sendBufferedUpdates() {
        log.info("Sending buffered updates");
        canvasUpdates.forEach((canvasId, cells) -> {
            if (!cells.isEmpty()) {
                try {
                    UpdateMessage updateMessage = new UpdateMessage(
                        canvasId, 
                        new UpdatedCells(new ArrayList<>(cells))
                    );
                    socketKafkaTemplate.send(topicDb, updateMessage.toJson());
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
