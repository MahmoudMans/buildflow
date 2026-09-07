package com.buildflow.system;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/system")
public class SystemHealthController {

    @GetMapping("/health")
    public HealthResponse health() {

        return new HealthResponse(
            "buildflow-backend",
            "UP",
            "0.1.0",
            Instant.now()
        );
    }
}
