package com.isums.houseservice.infrastructures.kafkas;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
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

    public void publishTenantChanged(UUID houseId, UUID tenantUserId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("houseId", houseId.toString());
            payload.put("tenantUserId", tenantUserId != null ? tenantUserId.toString() : null);
            payload.put("changedAt", System.currentTimeMillis());
            kafkaTemplate.send("house.tenant-changed", houseId.toString(), objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.warn("[KAFKA] publishTenantChanged failed houseId={}: {}", houseId, e.getMessage());
        }
    }
}
