package ru.collaborative_editor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.collaborative_editor.model.Cell;
import ru.collaborative_editor.model.UpdateMessage;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaUpdateMessageListener {
    private final SimpMessagingTemplate messagingTemplate;

    //listen messages from kafka and send them to clients
    @KafkaListener(topics = "${spring.kafka.topic-socket}", groupId = "${spring.kafka.group-id}")
    public void listen(@Payload String message) throws JsonProcessingException {
        UpdateMessage updateMessage = UpdateMessage.fromJson(message);
        String destination = "/canvas/" + updateMessage.canvasId();

        messagingTemplate.convertAndSend(
                destination,
                new ClientMessage(
                        updateMessage.updatedCells().value(),
                        updateMessage.senderId()
                )
        );
    }

    public record ClientMessage(
            List<Cell> pixels,
            String senderId
    ) {}
}
