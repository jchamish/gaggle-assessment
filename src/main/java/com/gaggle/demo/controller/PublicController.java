package com.gaggle.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    @GetMapping("/info")
    public Map<String, String> info() {
        return Map.of(
            "app", "Gaggle Demo",
            "version", "1.0.0",
            "status", "UP"
        );
    }
}