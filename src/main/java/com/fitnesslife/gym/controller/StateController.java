package com.fitnesslife.gym.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/state")
public class StateController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getState() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "app", "Fitness Life"
        ));
    }
}
