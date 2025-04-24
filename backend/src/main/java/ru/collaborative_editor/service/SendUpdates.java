package ru.collaborative_editor.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import ru.collaborative_editor.model.Cell;
import ru.collaborative_editor.model.UpdateMessage;
import ru.collaborative_editor.model.UpdatedCells;

import java.util.*;
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
    private final Map<String, Set<String>> chunkCache = new ConcurrentHashMap<>();


    // Send immediate updates for real-time rendering
    public void sendRealtimeUpdate(
            String canvasId,
            UpdatedCells cells,
            String senderId
    ) throws JsonProcessingException {
        List<Cell> pixels = cells.value();
        List<List<Cell>> chunks = partitionCells(pixels, 200);

        for (List<Cell> chunk : chunks) {
            String chunkKey = senderId + "-" + System.currentTimeMillis();
            if (!chunkCache.computeIfAbsent(canvasId, k -> new HashSet<>()).contains(chunkKey)) {
                UpdateMessage chunkMessage = new UpdateMessage(
                        canvasId,
                        new UpdatedCells(chunk),
                        senderId
                );
                socketKafkaTemplate.send(topicSocket, chunkMessage.toJson());
                chunkCache.get(canvasId).add(chunkKey);
            }
        }
    }

    private List<List<Cell>> partitionCells(List<Cell> cells, int size) {
        List<List<Cell>> chunks = new ArrayList<>();
        for (int i = 0; i < cells.size(); i += size) {
            chunks.add(cells.subList(i, Math.min(i + size, cells.size())));
        }
        return chunks;
    }

    @Scheduled(fixedRate = 300000)
    public void cleanupChunkCache() {
        chunkCache.clear();
    }

    //TODO: implement batch processing for sending updates to db
//    @Scheduled(fixedRateString = "${update.interval}", timeUnit = TimeUnit.SECONDS)
//    public void sendBufferedUpdates() {
//        log.info("Sending buffered updates");
//        canvasUpdates.forEach((canvasId, cells) -> {
//            if (!cells.isEmpty()) {
//                try {
//                    UpdateMessage updateMessage = new UpdateMessage(
//                        canvasId,
//                        new UpdatedCells(new ArrayList<>(cells))
//                    );
//                    socketKafkaTemplate.send(topicDb, updateMessage.toJson());
//                    cells.clear(); // Clear after sending
//                } catch (JsonProcessingException e) {
//                    // Handle error
//                }
//            }
//        });
//    }

    public List<Cell> createOrGetCanvasById(String canvasId) {
        return canvasUpdates.computeIfAbsent(canvasId, k -> 
            new CopyOnWriteArrayList<>());
    }

}
