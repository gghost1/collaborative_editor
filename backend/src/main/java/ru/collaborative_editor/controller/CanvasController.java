package ru.collaborative_editor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import ru.collaborative_editor.model.UpdateMessage;
import ru.collaborative_editor.model.UpdatedCells;

@Controller
@RequiredArgsConstructor
public class CanvasController {

    private final KafkaTemplate<String, String> socketKafkaTemplate;

    @MessageMapping("/draw/{canvasId}")
    public void handleDraw(@Payload UpdatedCells updatedCells, @DestinationVariable String canvasId) throws JsonProcessingException {
        UpdateMessage updateMessage = new UpdateMessage(canvasId, updatedCells);
        // push to the microservice

        socketKafkaTemplate.send("ws-draw", updateMessage.toJson());
    }

}
