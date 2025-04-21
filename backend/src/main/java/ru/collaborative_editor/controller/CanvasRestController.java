package ru.collaborative_editor.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/")
@Slf4j
public class CanvasRestController {


    @GetMapping("/{canvasId}")
    public String getCanvas(@PathVariable String canvasId) {
        log.info("Getting canvas with id in controller: {}", canvasId);
        return "Hello World";
    }
    
}
