package ru.collaborative_editor.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record Cell(
        int x,
        int y,
        String color
) {
}
