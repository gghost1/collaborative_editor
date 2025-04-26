package ru.collaborative_editor.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.annotation.PostConstruct;
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

    //TODO: remove this method(I do it myself)
    @PostConstruct
    public void initializeTopics() {
        log.info("Initializing Kafka topics on startup");
        
        // Create an initial empty update to ensure the topic is created
        try {
            // Create a dummy message to establish the topic
            UpdateMessage initialMessage = new UpdateMessage(
                "system", 
                new UpdatedCells(Collections.emptyList()),
                "system-init"
            );
            
            // Send to both topics to ensure they get created
            socketKafkaTemplate.send(topicSocket, initialMessage.toJson());
            socketKafkaTemplate.send(topicDb, initialMessage.toJson());
            
            log.info("Successfully initialized Kafka topics: {}, {}", topicSocket, topicDb);
        } catch (Exception e) {
            log.error("Failed to initialize Kafka topics", e);
        }
        
        // Also run the scheduled method immediately to process any existing data
        sendBufferedUpdates();
    }

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
    // send updates to database every YOU_CHOICE minutes, process by chuncks of 200 pixels
    @Scheduled(fixedRateString = "${update.interval}", timeUnit = TimeUnit.SECONDS)
    public void sendBufferedUpdates() {
        log.info("Sending buffered updates to database");
        canvasUpdates.forEach((canvasId, cells) -> {
            if (!cells.isEmpty()) {
                try {
                    // Get a copy of current cells to avoid modification during processing
                    List<Cell> cellsCopy = new ArrayList<>(cells);
                    
                    // Use the same partitioning logic as in real-time updates
                    List<List<Cell>> chunks = partitionCells(cellsCopy, 200);
                    
                    for (List<Cell> chunk : chunks) {
                        UpdateMessage updateMessage = new UpdateMessage(
                            canvasId,
                            new UpdatedCells(chunk),
                            "system-db-update" // don't need to send senderId
                        );
                        
                        // Send to database topic
                        log.info("Sending {} cells to database for canvas {}", chunk.size(), canvasId);
                        socketKafkaTemplate.send(topicDb, updateMessage.toJson());
                    }
                    
                    // Clear after sending
                    cells.clear();
                } catch (JsonProcessingException e) {
                    log.error("Error serializing update for database: {}", e.getMessage(), e);
                }
            }
        });
    }

    public List<Cell> createOrGetCanvasById(String canvasId) {
        return canvasUpdates.computeIfAbsent(canvasId, k -> 
            new CopyOnWriteArrayList<>());
    }

}
