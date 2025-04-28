package ru.collaborative_editor.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record UpdateMessage(
        String canvasId,
        UpdatedCells updatedCells,
        String senderId
) {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public String toJson() throws JsonProcessingException {
        return objectMapper.writeValueAsString(this);
    }

    public static UpdateMessage fromJson(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, UpdateMessage.class);
    }
}
