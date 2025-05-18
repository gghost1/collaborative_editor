package ru.collaborative_editor.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@Slf4j
public class CanvasRestController {
    private final Counter messageCounter;
    private DataSource dataSource;

    public CanvasRestController(DataSource dataSource, MeterRegistry registry) {
        this.dataSource = dataSource;
        this.messageCounter = Counter.builder("app.user.messages")
                .description("Количество сообщений от пользователей")
                .tags("application", "whiteboard")
                .register(registry);
    }

    //get canvas data from database
    @GetMapping("/{canvasId}")
    public ResponseEntity<String> getCanvas(@PathVariable String canvasId) {
        messageCounter.increment();
        log.info("Getting canvas with id: {}", canvasId);
        
        try (Connection conn = dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT data FROM frames WHERE canvas_id = ?")) {
            
            log.info("Connection to database in rest controller is established");
            stmt.setString(1, canvasId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String pixelData = rs.getString("data");
                String formattedResponse = String.format("{\"pixels\":%s, \"senderId\":\"%s\"}", pixelData, canvasId);

                log.info("Canvas data fetched successfully");
                return ResponseEntity.ok(formattedResponse);
            } else {
                log.info("No data found for canvas with id: {}", canvasId);
                String formattedResponse = String.format("{\"pixels\":[], \"senderId\":\"%s\"}", canvasId);
                return ResponseEntity.ok(formattedResponse); // Return empty array if no data
            }
        } catch (SQLException e) {
            log.error("Error fetching canvas data from database", e);
            return ResponseEntity.internalServerError().body("Error fetching canvas data");
        }
    }
}