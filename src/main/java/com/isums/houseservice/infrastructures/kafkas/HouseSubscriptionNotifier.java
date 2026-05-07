package com.isums.houseservice.infrastructures.kafkas;

import com.isums.houseservice.domains.entities.House;
import com.isums.houseservice.domains.entities.HouseSubscription;
import com.isums.houseservice.infrastructures.repositories.HouseSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class HouseSubscriptionNotifier {

    private static final String TOPIC = "notification-email";
    private static final String TEMPLATE = "house_available_notify";

    private final HouseSubscriptionRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void notifyAvailable(House house) {
        if (house == null) return;
        List<HouseSubscription> subs = repository.findByHouseIdAndNotifiedAtIsNull(house.getId());
        if (subs.isEmpty()) return;

        Instant now = Instant.now();
        for (HouseSubscription sub : subs) {
            if (sub.getUserEmail() == null || sub.getUserEmail().isBlank()) continue;
            try {
                String payload = objectMapper.writeValueAsString(Map.of(
                        "messageId", java.util.UUID.randomUUID().toString(),
                        "to", sub.getUserEmail(),
                        "templateCode", TEMPLATE,
                        "params", Map.of(
                                "houseName", house.getName() != null ? house.getName() : "",
                                "address", house.getAddress() != null ? house.getAddress() : "",
                                "houseId", house.getId().toString())));
                kafkaTemplate.send(TOPIC, sub.getId().toString(), payload);
                sub.setNotifiedAt(now);
                repository.save(sub);
            } catch (Exception e) {
                log.warn("[Subscription] notify failed houseId={} subscriberId={}: {}",
                        house.getId(), sub.getId(), e.getMessage());
            }
        }
        log.info("[Subscription] Notified {} subscribers for houseId={}",
                subs.size(), house.getId());
    }
}
