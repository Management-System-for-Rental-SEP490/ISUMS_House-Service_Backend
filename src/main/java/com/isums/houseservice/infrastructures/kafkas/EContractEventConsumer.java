package com.isums.houseservice.infrastructures.kafkas;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isums.houseservice.domains.events.ContractCancelledByTenantEvent;
import com.isums.houseservice.domains.events.ContractDepositExpiredEvent;
import com.isums.houseservice.domains.events.ContractReplacedEvent;
import com.isums.houseservice.domains.events.ContractTerminatedEvent;
import com.isums.houseservice.domains.events.InspectionDoneNotifyEvent;
import com.isums.houseservice.domains.events.MapUserToHouseEvent;
import com.isums.houseservice.infrastructures.abstracts.HouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EContractEventConsumer {

    private static final String GROUP = "house-group";

    private final HouseService houseService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "map-user-to-house-topic", groupId = GROUP,
            properties = {"auto.offset.reset:earliest"})
    public void handleMapUserToHouse(String payload) {
        log.info("[KAFKA] map-user-to-house ENTRY len={}", payload != null ? payload.length() : -1);
        if (payload == null) {
            log.error("[KAFKA] map-user-to-house null payload, skipping");
            return;
        }
        MapUserToHouseEvent event;
        try {
            event = objectMapper.readValue(payload, MapUserToHouseEvent.class);
        } catch (JacksonException e) {
            log.error("[KAFKA] map-user-to-house deserialize failed raw={}: {}", payload, e.getMessage());
            return;
        }
        try {
            houseService.activeHouseForUser(event.getUserId(), event.getHouseId(), event.getHandoverDate());
            log.info("[KAFKA] Mapped userId={} houseId={}", event.getUserId(), event.getHouseId());
        } catch (Exception e) {
            log.warn("[KAFKA] handleMapUserToHouse failed userId={} houseId={} - will retry: {}",
                    event.getUserId(), event.getHouseId(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "contract.terminated", groupId = GROUP,
            properties = {"auto.offset.reset:earliest"})
    public void handleContractTerminated(String payload) {
        log.info("[KAFKA] contract.terminated ENTRY len={}", payload != null ? payload.length() : -1);
        if (payload == null) return;
        ContractTerminatedEvent event;
        try {
            event = objectMapper.readValue(payload, ContractTerminatedEvent.class);
        } catch (JacksonException e) {
            log.error("[KAFKA] contract.terminated deserialize failed raw={}: {}", payload, e.getMessage());
            return;
        }
        try {
            houseService.deactivateHouseForUser(event.getTenantId(), event.getHouseId(), false);
            log.info("[KAFKA] Contract terminated houseId={} tenantId={}",
                    event.getHouseId(), event.getTenantId());
        } catch (Exception e) {
            log.warn("[KAFKA] handleContractTerminated failed - will retry: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "contract.access-ended", groupId = GROUP,
            properties = {"auto.offset.reset:earliest"})
    public void handleContractAccessEnded(String payload) {
        log.info("[KAFKA] contract.access-ended ENTRY len={}", payload != null ? payload.length() : -1);
        if (payload == null) return;
        ContractTerminatedEvent event;
        try {
            event = objectMapper.readValue(payload, ContractTerminatedEvent.class);
        } catch (JacksonException e) {
            log.error("[KAFKA] contract.access-ended deserialize failed raw={}: {}", payload, e.getMessage());
            return;
        }
        try {
            houseService.revokeHouseAccessForUser(event.getTenantId(), event.getHouseId());
            log.info("[KAFKA] Contract access ended houseId={} tenantId={}",
                    event.getHouseId(), event.getTenantId());
        } catch (Exception e) {
            log.warn("[KAFKA] handleContractAccessEnded failed - will retry: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "contract.replaced", groupId = GROUP,
            properties = {"auto.offset.reset:earliest"})
    public void handleContractReplaced(String payload) {
        log.info("[KAFKA] contract.replaced ENTRY len={}", payload != null ? payload.length() : -1);
        if (payload == null) return;
        ContractReplacedEvent event;
        try {
            event = objectMapper.readValue(payload, ContractReplacedEvent.class);
        } catch (JacksonException e) {
            log.error("[KAFKA] contract.replaced deserialize failed raw={}: {}", payload, e.getMessage());
            return;
        }
        if (event.getOldHouseId() == null || event.getTenantId() == null) {
            log.warn("[KAFKA] contract.replaced missing oldHouseId/tenantId msgId={} oldHouseId={} tenantId={}",
                    event.getMessageId(), event.getOldHouseId(), event.getTenantId());
            return;
        }
        log.info("[KAFKA] contract.replaced received msgId={} oldContractId={} oldHouseId={} tenantId={} keepUnavailable={} reason={}",
                event.getMessageId(), event.getOldContractId(), event.getOldHouseId(),
                event.getTenantId(), event.isKeepHouseUnavailable(), event.getReason());

        try {
            houseService.deactivateHouseForUser(
                    event.getTenantId(),
                    event.getOldHouseId(),
                    event.isKeepHouseUnavailable());
            if (event.getNewHouseId() != null) {
                houseService.activeHouseForUser(
                        event.getTenantId(),
                        event.getNewHouseId(),
                        event.getNewHandoverDate() != null
                                ? event.getNewHandoverDate()
                                : event.getReplacedAt());
            }
            log.info("[KAFKA] contract.replaced processed msgId={} oldHouseId={} newHouseId={}",
                    event.getMessageId(), event.getOldHouseId(), event.getNewHouseId());
        } catch (Exception e) {
            log.warn("[KAFKA] handleContractReplaced failed msgId={} - will retry: {}",
                    event.getMessageId(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "contract.cancelled-by-tenant", groupId = GROUP,
            properties = {"auto.offset.reset:earliest"})
    public void handleContractCancelledByTenant(String payload) {
        log.info("[KAFKA] contract.cancelled-by-tenant ENTRY len={}", payload != null ? payload.length() : -1);
        if (payload == null) return;
        ContractCancelledByTenantEvent event;
        try {
            event = objectMapper.readValue(payload, ContractCancelledByTenantEvent.class);
        } catch (JacksonException e) {
            log.error("[KAFKA] contract.cancelled-by-tenant deserialize failed raw={}: {}",
                    payload, e.getMessage());
            return;
        }
        if (event.getHouseId() == null || event.getTenantId() == null) {
            log.warn("[KAFKA] contract.cancelled-by-tenant missing houseId/tenantId, skip");
            return;
        }
        try {
            houseService.deactivateHouseForUser(event.getTenantId(), event.getHouseId(), false);
            log.info("[KAFKA] Contract cancelled by tenant houseId={} tenantId={}",
                    event.getHouseId(), event.getTenantId());
        } catch (Exception e) {
            log.warn("[KAFKA] handleContractCancelledByTenant failed - will retry: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "contract.inspection.done", groupId = GROUP,
            properties = {"auto.offset.reset:earliest"})
    public void handleInspectionDone(String payload) {
        log.info("[KAFKA] contract.inspection.done ENTRY len={}", payload != null ? payload.length() : -1);
        if (payload == null) return;
        InspectionDoneNotifyEvent event;
        try {
            event = objectMapper.readValue(payload, InspectionDoneNotifyEvent.class);
        } catch (JacksonException e) {
            log.error("[KAFKA] contract.inspection.done deserialize failed raw={}: {}",
                    payload, e.getMessage());
            return;
        }
        if (event.getHouseId() == null || event.getTenantId() == null) {
            log.warn("[KAFKA] contract.inspection.done missing houseId/tenantId msgId={}",
                    event.getMessageId());
            return;
        }
        log.info("[KAFKA] contract.inspection.done received msgId={} contractId={} houseId={} tenantId={}",
                event.getMessageId(), event.getContractId(), event.getHouseId(), event.getTenantId());
        try {
            boolean demo = event.getEffectiveAt() != null;
            houseService.completeCheckoutAndHandover(
                    event.getTenantId(),
                    event.getHouseId(),
                    demo ? event.getEffectiveAt() : java.time.Instant.now(),
                    demo);
            log.info("[KAFKA] contract.inspection.done processed msgId={} houseId={}",
                    event.getMessageId(), event.getHouseId());
        } catch (Exception e) {
            log.warn("[KAFKA] handleInspectionDone failed msgId={} - will retry: {}",
                    event.getMessageId(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "contract.deposit-expired", groupId = GROUP,
            properties = {"auto.offset.reset:earliest"})
    public void handleContractDepositExpired(String payload) {
        log.info("[KAFKA] contract.deposit-expired ENTRY len={}", payload != null ? payload.length() : -1);
        if (payload == null) {
            log.error("[KAFKA] contract.deposit-expired null payload, skipping");
            return;
        }
        ContractDepositExpiredEvent event;
        try {
            event = objectMapper.readValue(payload, ContractDepositExpiredEvent.class);
        } catch (JacksonException e) {
            log.error("[KAFKA] contract.deposit-expired deserialize failed raw={}: {}",
                    payload, e.getMessage());
            return;
        }
        if (event.getHouseId() == null || event.getTenantId() == null) {
            log.warn("[KAFKA] contract.deposit-expired missing houseId/tenantId msgId={} contractId={}",
                    event.getMessageId(), event.getContractId());
            return;
        }
        log.info("[KAFKA] contract.deposit-expired received msgId={} contractId={} houseId={} tenantId={}",
                event.getMessageId(), event.getContractId(),
                event.getHouseId(), event.getTenantId());
        try {
            houseService.deactivateHouseForUser(event.getTenantId(), event.getHouseId(), false);
            log.info("[KAFKA] contract.deposit-expired processed msgId={} houseId={} -> released",
                    event.getMessageId(), event.getHouseId());
        } catch (Exception e) {
            log.warn("[KAFKA] handleContractDepositExpired failed - will retry: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
