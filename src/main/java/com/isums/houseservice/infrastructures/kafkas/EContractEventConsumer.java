package com.isums.houseservice.infrastructures.kafkas;

import com.isums.houseservice.domains.events.MapUserToHouseEvent;
import com.isums.houseservice.infrastructures.abstracts.HouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EContractEventConsumer {

    private final HouseService houseService;

    @KafkaListener(topics = "map-user-to-house-topic", groupId = "house-group")
    public void handleMapUserToHouse(MapUserToHouseEvent event) {
        try {
            houseService.activeHouseForUser(event.getUserId(), event.getHouseId());
        } catch (Exception e) {
            log.error("[KAFKA] handleMapUserToHouse failed: {}", e.getMessage());
        }
    }
}
