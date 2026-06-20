package com.creditflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the CreditFlow AI backend.
 *
 * <p>The backend is the system of record and the orchestrator: it owns
 * persistence and the API contract, and it drives the Python agent service.
 * {@code @EnableAsync} lets uploads return immediately while the multi-agent
 * workflow runs on a background thread.
 */
@EnableAsync
@SpringBootApplication
public class CreditFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreditFlowApplication.class, args);
    }
}
