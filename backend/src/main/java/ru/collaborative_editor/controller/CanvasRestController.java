package ru.collaborative_editor.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/")
public class CanvasRestController {


    @GetMapping("/{canvasId}")
    public String getCanvas(@PathVariable String canvasId) {
        return "Hello World";
    }
    
}
