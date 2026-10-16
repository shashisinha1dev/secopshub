package com.secopshub.alert.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/api/v1/alerts/health")
    public Map<String, String> health() {
        return Map.of("service", "alert-ingestion-service", "status", "UP");
    }
}
