package ru.collaborative_editor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.collaborative_editor.model.UpdateMessage;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaUpdateMessageListener {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "${spring.kafka.topic}", groupId = "${spring.kafka.group-id}")
    public void listen(@Payload String message) throws JsonProcessingException {
        UpdateMessage updateMessage = UpdateMessage.fromJson(message);
        String destination = "/canvas/" + updateMessage.canvasId();
        log.info("Sending message to destination in listener: {}", destination);

        messagingTemplate.convertAndSend(destination, updateMessage.updatedCells().value());
    }

}
