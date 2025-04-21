package ru.collaborative_editor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ru.collaborative_editor.model.UpdateMessage;

@Component
@RequiredArgsConstructor
public class KafkaUpdateMessageListener {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "ws-draw", groupId = "${spring.kafka.group-id}")
    public void listen(String message) throws JsonProcessingException {
        UpdateMessage updateMessage = UpdateMessage.fromJson(message);
        String destination = "/canvas/" + updateMessage.canvasId();

        messagingTemplate.convertAndSend(destination, updateMessage.updatedCells().value());
    }

}
