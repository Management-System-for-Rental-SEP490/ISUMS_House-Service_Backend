package com.isums.houseservice.infrastructures.kafkas;

import com.isums.houseservice.domains.events.ContractCancelledByTenantEvent;
import com.isums.houseservice.domains.events.ContractReplacedEvent;
import com.isums.houseservice.domains.events.ContractTerminatedEvent;
import com.isums.houseservice.domains.events.InspectionDoneNotifyEvent;
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
        log.info("[KAFKA] map-user-to-house received partition={} offset={}",
                record.partition(), record.offset());
        try {
            MapUserToHouseEvent event = objectMapper.readValue(record.value(), MapUserToHouseEvent.class);
            log.info("[KAFKA] map-user-to-house parsed userId={} houseId={} handoverDate={}",
                    event.getUserId(), event.getHouseId(), event.getHandoverDate());
            houseService.activeHouseForUser(event.getUserId(), event.getHouseId(), event.getHandoverDate());
            log.info("[KAFKA] Mapped userId={} houseId={}", event.getUserId(), event.getHouseId());
            ack.acknowledge();
        } catch (tools.jackson.core.JacksonException e) {
            log.error("[KAFKA] Deserialize failed partition={} offset={} raw={}: {}",
                    record.partition(), record.offset(), record.value(), e.getMessage());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[KAFKA] handleMapUserToHouse failed partition={} offset={}: {}",
                    record.partition(), record.offset(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "contract.terminated", groupId = "house-group")
    public void handleContractTerminated(
            ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            ContractTerminatedEvent event = objectMapper.readValue(record.value(), ContractTerminatedEvent.class);

            houseService.deactivateHouseForUser(event.getTenantId(), event.getHouseId(), false);

            ack.acknowledge();
            log.info("[KAFKA] Contract terminated houseId={} tenantId={}",
                    event.getHouseId(), event.getTenantId());
        } catch (Exception e) {
            log.error("[KAFKA] handleContractTerminated failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "contract.replaced", groupId = "house-group")
    public void handleContractReplaced(
            ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            ContractReplacedEvent event = objectMapper.readValue(record.value(), ContractReplacedEvent.class);

            if (event.getOldHouseId() == null || event.getTenantId() == null) {
                log.warn("[KAFKA] contract.replaced missing oldHouseId/tenantId msgId={} oldHouseId={} tenantId={}",
                        event.getMessageId(), event.getOldHouseId(), event.getTenantId());
                ack.acknowledge();
                return;
            }

            log.info("[KAFKA] contract.replaced received msgId={} oldContractId={} oldHouseId={} tenantId={} keepUnavailable={} reason={}",
                    event.getMessageId(), event.getOldContractId(), event.getOldHouseId(),
                    event.getTenantId(), event.isKeepHouseUnavailable(), event.getReason());

            houseService.deactivateHouseForUser(
                    event.getTenantId(),
                    event.getOldHouseId(),
                    event.isKeepHouseUnavailable());

            log.info("[KAFKA] contract.replaced processed msgId={} oldHouseId={}",
                    event.getMessageId(), event.getOldHouseId());
            ack.acknowledge();
        } catch (tools.jackson.core.JacksonException e) {
            log.error("[KAFKA] contract.replaced deserialize failed partition={} offset={} raw={}: {}",
                    record.partition(), record.offset(), record.value(), e.getMessage());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[KAFKA] handleContractReplaced failed partition={} offset={}: {}",
                    record.partition(), record.offset(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "contract.cancelled-by-tenant", groupId = "house-group")
    public void handleContractCancelledByTenant(
            ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            ContractCancelledByTenantEvent event = objectMapper.readValue(
                    record.value(), ContractCancelledByTenantEvent.class);

            if (event.getHouseId() != null && event.getTenantId() != null) {
                houseService.deactivateHouseForUser(
                        event.getTenantId(),
                        event.getHouseId(),
                        false);
            }

            ack.acknowledge();
            log.info("[KAFKA] Contract cancelled by tenant houseId={} tenantId={}",
                    event.getHouseId(), event.getTenantId());
        } catch (Exception e) {
            log.error("[KAFKA] handleContractCancelledByTenant failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "contract.inspection.done", groupId = "house-group")
    public void handleInspectionDone(
            ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            InspectionDoneNotifyEvent event = objectMapper.readValue(
                    record.value(), InspectionDoneNotifyEvent.class);

            if (event.getHouseId() == null || event.getTenantId() == null) {
                log.warn("[KAFKA] contract.inspection.done missing houseId/tenantId msgId={}",
                        event.getMessageId());
                ack.acknowledge();
                return;
            }

            log.info("[KAFKA] contract.inspection.done received msgId={} contractId={} houseId={} tenantId={}",
                    event.getMessageId(), event.getContractId(), event.getHouseId(), event.getTenantId());

            houseService.deactivateHouseForUser(
                    event.getTenantId(),
                    event.getHouseId(),
                    false);

            ack.acknowledge();
            log.info("[KAFKA] contract.inspection.done processed msgId={} houseId={}",
                    event.getMessageId(), event.getHouseId());
        } catch (tools.jackson.core.JacksonException e) {
            log.error("[KAFKA] contract.inspection.done deserialize failed raw={}: {}",
                    record.value(), e.getMessage());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[KAFKA] handleInspectionDone failed: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
