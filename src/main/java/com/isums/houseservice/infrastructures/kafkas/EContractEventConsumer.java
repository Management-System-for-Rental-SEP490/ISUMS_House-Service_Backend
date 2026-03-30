package com.isums.houseservice.infrastructures.kafkas;

import com.isums.houseservice.domains.events.MapUserToHouseEvent;
import com.isums.houseservice.infrastructures.abstracts.HouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class EContractEventConsumer {

    private final HouseService houseService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "map-user-to-house-topic", groupId = "house-group")
    public void handleMapUserToHouse(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            MapUserToHouseEvent event = objectMapper.readValue(record.value(), MapUserToHouseEvent.class);
            houseService.activeHouseForUser(event.getUserId(), event.getHouseId(), event.getHandoverDate());
            ack.acknowledge();
            log.info("[KAFKA] Mapped userId={} houseId={}", event.getUserId(), event.getHouseId());
        } catch (tools.jackson.core.JacksonException e) {
            log.error("[KAFKA] Deserialize failed raw={}: {}", record.value(), e.getMessage());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[KAFKA] handleMapUserToHouse failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
