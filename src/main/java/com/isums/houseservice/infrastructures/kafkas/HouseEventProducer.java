package com.isums.houseservice.infrastructures.kafkas;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class HouseEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishHouseCreated(UUID houseId) {
        try {
            String payload = objectMapper.writeValueAsString(
                    Map.of("houseId", houseId.toString())
            );
            kafkaTemplate.send("house.created", houseId.toString(), payload);
        } catch (Exception e) {
            log.warn("[KAFKA] publishHouseCreated failed: {}", e.getMessage());
        }
    }
}
