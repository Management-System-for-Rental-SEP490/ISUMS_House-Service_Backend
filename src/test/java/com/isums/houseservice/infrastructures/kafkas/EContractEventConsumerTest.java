package com.isums.houseservice.infrastructures.kafkas;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isums.houseservice.domains.events.ContractCancelledByTenantEvent;
import com.isums.houseservice.domains.events.ContractDepositExpiredEvent;
import com.isums.houseservice.domains.events.ContractReplacedEvent;
import com.isums.houseservice.domains.events.ContractTerminatedEvent;
import com.isums.houseservice.domains.events.InspectionDoneNotifyEvent;
import com.isums.houseservice.domains.events.MapUserToHouseEvent;
import com.isums.houseservice.domains.events.RenewalWindowOpenEvent;
import com.isums.houseservice.infrastructures.abstracts.HouseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EContractEventConsumer")
class EContractEventConsumerTest {

    @Mock private HouseService houseService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private EContractEventConsumer consumer;

    @Nested
    @DisplayName("handleMapUserToHouse")
    class HandleMapUserToHouse {

        @Test
        @DisplayName("maps user to house on happy path")
        void happyPath() throws Exception {
            MapUserToHouseEvent event = MapUserToHouseEvent.builder()
                    .userId(UUID.randomUUID()).houseId(UUID.randomUUID())
                    .handoverDate(Instant.now()).build();
            when(objectMapper.readValue("payload", MapUserToHouseEvent.class)).thenReturn(event);

            consumer.handleMapUserToHouse("payload");

            verify(houseService).activeHouseForUser(event.getUserId(), event.getHouseId(), event.getHandoverDate());
        }

        @Test
        @DisplayName("swallows JSON parse error (poison-pill, no retry)")
        void badJson() throws Exception {
            when(objectMapper.readValue(any(String.class), eq(MapUserToHouseEvent.class)))
                    .thenThrow(new JsonParseException(null, "bad"));

            consumer.handleMapUserToHouse("payload");

            verifyNoInteractions(houseService);
        }

        @Test
        @DisplayName("swallows null payload (no work, no NPE)")
        void nullPayload() {
            consumer.handleMapUserToHouse(null);
            verifyNoInteractions(houseService);
        }

        @Test
        @DisplayName("rethrows RuntimeException when downstream service fails (for retry)")
        void downstreamFails() throws Exception {
            MapUserToHouseEvent event = MapUserToHouseEvent.builder()
                    .userId(UUID.randomUUID()).houseId(UUID.randomUUID())
                    .handoverDate(Instant.now()).build();
            when(objectMapper.readValue("payload", MapUserToHouseEvent.class)).thenReturn(event);
            doThrow(new RuntimeException("house service down"))
                    .when(houseService).activeHouseForUser(any(), any(), any());

            assertThatThrownBy(() -> consumer.handleMapUserToHouse("payload"))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("handleContractTerminated")
    class HandleContractTerminated {

        @Test
        @DisplayName("releases house on happy path")
        void happyPath() throws Exception {
            ContractTerminatedEvent event = new ContractTerminatedEvent();
            event.setTenantId(UUID.randomUUID());
            event.setHouseId(UUID.randomUUID());
            when(objectMapper.readValue("v", ContractTerminatedEvent.class)).thenReturn(event);

            consumer.handleContractTerminated("v");

            verify(houseService).deactivateHouseForUser(event.getTenantId(), event.getHouseId(), false);
        }

        @Test
        @DisplayName("swallows null payload")
        void nullPayload() {
            consumer.handleContractTerminated(null);
            verifyNoInteractions(houseService);
        }

        @Test
        @DisplayName("swallows JSON parse error (no retry)")
        void badJson() throws Exception {
            when(objectMapper.readValue(any(String.class), eq(ContractTerminatedEvent.class)))
                    .thenThrow(new JsonParseException(null, "bad"));

            consumer.handleContractTerminated("v");

            verifyNoInteractions(houseService);
        }

        @Test
        @DisplayName("rethrows on downstream failure for retry")
        void downstreamFails() throws Exception {
            ContractTerminatedEvent event = new ContractTerminatedEvent();
            event.setTenantId(UUID.randomUUID());
            event.setHouseId(UUID.randomUUID());
            when(objectMapper.readValue("v", ContractTerminatedEvent.class)).thenReturn(event);
            doThrow(new RuntimeException("db down"))
                    .when(houseService).deactivateHouseForUser(any(), any(), anyBoolean());

            assertThatThrownBy(() -> consumer.handleContractTerminated("v"))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("handleContractReplaced (đổi nhà)")
    class HandleContractReplaced {

        private ContractReplacedEvent event(boolean keepUnavailable) {
            ContractReplacedEvent e = new ContractReplacedEvent();
            e.setMessageId(UUID.randomUUID().toString());
            e.setOldContractId(UUID.randomUUID());
            e.setNewContractId(UUID.randomUUID());
            e.setOldHouseId(UUID.randomUUID());
            e.setNewHouseId(UUID.randomUUID());
            e.setTenantId(UUID.randomUUID());
            e.setKeepHouseUnavailable(keepUnavailable);
            e.setDepositHandling("TRANSFER_TO_REPLACEMENT");
            e.setTransferredDepositAmount(5_000_000L);
            e.setReason("Tenant upgrade");
            e.setReplacedAt(Instant.now());
            e.setNewHandoverDate(Instant.now().plusSeconds(3600));
            return e;
        }

        @Test
        @DisplayName("tenant-upgrade (keepUnavailable=false) — releases old house and maps replacement house")
        void tenantUpgradeReleasesOldAndMapsNewHouse() throws Exception {
            ContractReplacedEvent evt = event(false);
            when(objectMapper.readValue("v", ContractReplacedEvent.class)).thenReturn(evt);

            consumer.handleContractReplaced("v");

            InOrder order = inOrder(houseService);
            order.verify(houseService).deactivateHouseForUser(evt.getTenantId(), evt.getOldHouseId(), false);
            order.verify(houseService).activeHouseForUser(
                    evt.getTenantId(), evt.getNewHouseId(), evt.getNewHandoverDate());
        }

        @Test
        @DisplayName("landlord-fault (keepUnavailable=true) — locks old house and maps replacement house")
        void landlordFaultLocksOldAndMapsNewHouse() throws Exception {
            ContractReplacedEvent evt = event(true);
            when(objectMapper.readValue("v", ContractReplacedEvent.class)).thenReturn(evt);

            consumer.handleContractReplaced("v");

            InOrder order = inOrder(houseService);
            order.verify(houseService).deactivateHouseForUser(evt.getTenantId(), evt.getOldHouseId(), true);
            order.verify(houseService).activeHouseForUser(
                    evt.getTenantId(), evt.getNewHouseId(), evt.getNewHandoverDate());
        }

        @Test
        @DisplayName("missing newHouseId — only releases old house")
        void missingNewHouseOnlyReleasesOldHouse() throws Exception {
            ContractReplacedEvent evt = event(false);
            evt.setNewHouseId(null);
            when(objectMapper.readValue("v", ContractReplacedEvent.class)).thenReturn(evt);

            consumer.handleContractReplaced("v");

            verify(houseService).deactivateHouseForUser(evt.getTenantId(), evt.getOldHouseId(), false);
            verify(houseService, never()).activeHouseForUser(any(), any(), any());
        }

        @Test
        @DisplayName("missing oldHouseId — skips processing")
        void missingOldHouseIdSkips() throws Exception {
            ContractReplacedEvent evt = event(false);
            evt.setOldHouseId(null);
            when(objectMapper.readValue("v", ContractReplacedEvent.class)).thenReturn(evt);

            consumer.handleContractReplaced("v");

            verify(houseService, never()).deactivateHouseForUser(any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("missing tenantId — skips processing")
        void missingTenantIdSkips() throws Exception {
            ContractReplacedEvent evt = event(false);
            evt.setTenantId(null);
            when(objectMapper.readValue("v", ContractReplacedEvent.class)).thenReturn(evt);

            consumer.handleContractReplaced("v");

            verify(houseService, never()).deactivateHouseForUser(any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("swallows JSON parse error on poison-pill")
        void poisonPill() throws Exception {
            when(objectMapper.readValue(any(String.class), eq(ContractReplacedEvent.class)))
                    .thenThrow(new JsonParseException(null, "bad"));

            consumer.handleContractReplaced("v");

            verifyNoInteractions(houseService);
        }

        @Test
        @DisplayName("rethrows on downstream service failure for retry")
        void downstreamFailureRetries() throws Exception {
            ContractReplacedEvent evt = event(false);
            when(objectMapper.readValue("v", ContractReplacedEvent.class)).thenReturn(evt);
            doThrow(new RuntimeException("house service down"))
                    .when(houseService).deactivateHouseForUser(any(), any(), anyBoolean());

            assertThatThrownBy(() -> consumer.handleContractReplaced("v"))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("replay invokes downstream twice (dedup is downstream's job)")
        void replayBoth() throws Exception {
            ContractReplacedEvent evt = event(false);
            when(objectMapper.readValue("v", ContractReplacedEvent.class)).thenReturn(evt);

            consumer.handleContractReplaced("v");
            consumer.handleContractReplaced("v");

            verify(houseService, org.mockito.Mockito.times(2))
                    .deactivateHouseForUser(evt.getTenantId(), evt.getOldHouseId(), false);
            verify(houseService, org.mockito.Mockito.times(2))
                    .activeHouseForUser(evt.getTenantId(), evt.getNewHouseId(), evt.getNewHandoverDate());
        }
    }

    @Nested
    @DisplayName("handleContractCancelledByTenant")
    class HandleContractCancelledByTenant {

        @Test
        @DisplayName("releases house on happy path")
        void happyPath() throws Exception {
            ContractCancelledByTenantEvent event = new ContractCancelledByTenantEvent();
            event.setTenantId(UUID.randomUUID());
            event.setHouseId(UUID.randomUUID());
            when(objectMapper.readValue("v", ContractCancelledByTenantEvent.class)).thenReturn(event);

            consumer.handleContractCancelledByTenant("v");

            verify(houseService).deactivateHouseForUser(event.getTenantId(), event.getHouseId(), false);
        }

        @Test
        @DisplayName("skips when houseId/tenantId missing")
        void missingFields() throws Exception {
            ContractCancelledByTenantEvent event = new ContractCancelledByTenantEvent();
            when(objectMapper.readValue("v", ContractCancelledByTenantEvent.class)).thenReturn(event);

            consumer.handleContractCancelledByTenant("v");

            verify(houseService, never()).deactivateHouseForUser(any(), any(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("handleInspectionDone")
    class HandleInspectionDone {

        @Test
        @DisplayName("releases house on happy path")
        void happyPath() throws Exception {
            InspectionDoneNotifyEvent event = new InspectionDoneNotifyEvent();
            event.setMessageId(UUID.randomUUID().toString());
            event.setContractId(UUID.randomUUID());
            event.setHouseId(UUID.randomUUID());
            event.setTenantId(UUID.randomUUID());
            when(objectMapper.readValue("v", InspectionDoneNotifyEvent.class)).thenReturn(event);

            consumer.handleInspectionDone("v");

            verify(houseService).completeCheckoutAndHandover(
                    eq(event.getTenantId()),
                    eq(event.getHouseId()),
                    any(java.time.Instant.class),
                    eq(false));
        }

        @Test
        @DisplayName("skips when houseId/tenantId missing")
        void missingFields() throws Exception {
            InspectionDoneNotifyEvent event = new InspectionDoneNotifyEvent();
            event.setMessageId("m");
            when(objectMapper.readValue("v", InspectionDoneNotifyEvent.class)).thenReturn(event);

            consumer.handleInspectionDone("v");

            verify(houseService, never()).completeCheckoutAndHandover(
                    any(), any(), any(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("handleContractDepositExpired")
    class HandleContractDepositExpired {

        @Test
        @DisplayName("releases house on happy path")
        void happyPath() throws Exception {
            UUID tenantId = UUID.randomUUID();
            UUID houseId = UUID.randomUUID();
            ContractDepositExpiredEvent event = new ContractDepositExpiredEvent();
            event.setMessageId(UUID.randomUUID().toString());
            event.setContractId(UUID.randomUUID());
            event.setHouseId(houseId);
            event.setTenantId(tenantId);
            when(objectMapper.readValue("payload", ContractDepositExpiredEvent.class))
                    .thenReturn(event);

            consumer.handleContractDepositExpired("payload");

            verify(houseService).releaseExpiredDeposit(tenantId, houseId);
        }

        @Test
        @DisplayName("swallows null payload")
        void nullPayload() {
            consumer.handleContractDepositExpired(null);
            verifyNoInteractions(houseService);
        }

        @Test
        @DisplayName("rethrows on downstream service failure")
        void downstreamFails() throws Exception {
            UUID tenantId = UUID.randomUUID();
            UUID houseId = UUID.randomUUID();
            ContractDepositExpiredEvent event = new ContractDepositExpiredEvent();
            event.setMessageId("m");
            event.setHouseId(houseId);
            event.setTenantId(tenantId);
            when(objectMapper.readValue("payload", ContractDepositExpiredEvent.class))
                    .thenReturn(event);
            doThrow(new RuntimeException("house service down"))
                    .when(houseService).releaseExpiredDeposit(eq(tenantId), eq(houseId));

            assertThatThrownBy(() -> consumer.handleContractDepositExpired("payload"))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("handleRenewalWindowOpen")
    class HandleRenewalWindowOpen {

        @Test
        @DisplayName("opens deposit window on happy path")
        void happyPath() throws Exception {
            UUID houseId = UUID.randomUUID();
            RenewalWindowOpenEvent event = RenewalWindowOpenEvent.builder()
                    .contractId(UUID.randomUUID()).houseId(houseId)
                    .messageId(UUID.randomUUID().toString()).build();
            when(objectMapper.readValue("payload", RenewalWindowOpenEvent.class)).thenReturn(event);

            consumer.handleRenewalWindowOpen("payload");

            verify(houseService).openDepositWindow(houseId);
        }

        @Test
        @DisplayName("swallows null payload")
        void nullPayload() {
            consumer.handleRenewalWindowOpen(null);
            verifyNoInteractions(houseService);
        }

        @Test
        @DisplayName("skips when houseId missing")
        void missingHouseId() throws Exception {
            RenewalWindowOpenEvent event = RenewalWindowOpenEvent.builder()
                    .messageId("m").build();
            when(objectMapper.readValue("payload", RenewalWindowOpenEvent.class)).thenReturn(event);

            consumer.handleRenewalWindowOpen("payload");

            verify(houseService, never()).openDepositWindow(any());
        }

        @Test
        @DisplayName("rethrows on downstream failure for retry")
        void downstreamFails() throws Exception {
            UUID houseId = UUID.randomUUID();
            RenewalWindowOpenEvent event = RenewalWindowOpenEvent.builder()
                    .houseId(houseId).messageId("m").build();
            when(objectMapper.readValue("payload", RenewalWindowOpenEvent.class)).thenReturn(event);
            doThrow(new RuntimeException("house service down"))
                    .when(houseService).openDepositWindow(houseId);

            assertThatThrownBy(() -> consumer.handleRenewalWindowOpen("payload"))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
