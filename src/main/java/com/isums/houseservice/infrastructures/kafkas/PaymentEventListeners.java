package com.isums.houseservice.infrastructures.kafkas;

import com.isums.houseservice.domains.events.AppAccessChangedEvent;
import com.isums.houseservice.infrastructures.abstracts.HouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentEventListeners {

    private final HouseService houseService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment.app-access-changed", groupId = "house-group")
    public void handleAppAccessChanged(
            ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            AppAccessChangedEvent event = objectMapper.readValue(
                    record.value(), AppAccessChangedEvent.class);

            houseService.setTenantAccessRestriction(
                    event.getTenantId(),
                    event.getHouseId(),
                    event.isRestricted()
            );

            ack.acknowledge();
            log.info("[House] AppAccess tenantId={} restricted={}",
                    event.getTenantId(), event.isRestricted());
        } catch (Exception e) {
            log.error("[House] handleAppAccessChanged failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
