package com.isums.houseservice.infrastructures.kafkas;

import com.isums.houseservice.domains.events.ContractTerminatedEvent;
import com.isums.houseservice.domains.events.MapUserToHouseEvent;
import com.isums.houseservice.infrastructures.abstracts.HouseService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EContractEventConsumer")
class EContractEventConsumerTest {

    @Mock private HouseService houseService;
    @Mock private ObjectMapper objectMapper;
    @Mock private Acknowledgment ack;

    @InjectMocks private EContractEventConsumer consumer;

    @Nested
    @DisplayName("handleMapUserToHouse")
    class HandleMapUserToHouse {

        private ConsumerRecord<String, String> rec = new ConsumerRecord<>(
                "map-user-to-house-topic", 0, 0L, "k", "payload");

        @Test
        @DisplayName("maps user to house and acknowledges on happy path")
        void happyPath() throws Exception {
            MapUserToHouseEvent event = MapUserToHouseEvent.builder()
                    .userId(UUID.randomUUID()).houseId(UUID.randomUUID())
                    .handoverDate(Instant.now()).build();
            when(objectMapper.readValue("payload", MapUserToHouseEvent.class)).thenReturn(event);

            consumer.handleMapUserToHouse(rec, ack);

            verify(houseService).activeHouseForUser(event.getUserId(), event.getHouseId(), event.getHandoverDate());
            verify(ack).acknowledge();
        }

        @Test
        @DisplayName("swallows JacksonException and acks (bad message, no retry)")
        void badJson() throws Exception {
            when(objectMapper.readValue(any(String.class), eq(MapUserToHouseEvent.class)))
                    .thenThrow(new JacksonException("bad") {});

            consumer.handleMapUserToHouse(rec, ack);

            verify(ack).acknowledge();
            verifyNoInteractions(houseService);
        }

        @Test
        @DisplayName("rethrows RuntimeException when downstream service fails (for retry)")
        void downstreamFails() throws Exception {
            MapUserToHouseEvent event = MapUserToHouseEvent.builder()
                    .userId(UUID.randomUUID()).houseId(UUID.randomUUID())
                    .handoverDate(Instant.now()).build();
            when(objectMapper.readValue("payload", MapUserToHouseEvent.class)).thenReturn(event);
            doThrow(new RuntimeException("service down"))
                    .when(houseService).activeHouseForUser(any(), any(), any());

            assertThatThrownBy(() -> consumer.handleMapUserToHouse(rec, ack))
                    .isInstanceOf(RuntimeException.class);
            verify(ack, never()).acknowledge();
        }
    }

    @Nested
    @DisplayName("handleContractTerminated")
    class HandleContractTerminated {

        private ConsumerRecord<String, String> rec = new ConsumerRecord<>(
                "contract.terminated", 0, 0L, "k", "payload");

        @Test
        @DisplayName("deactivates house and acknowledges on happy path")
        void happyPath() throws Exception {
            ContractTerminatedEvent event = new ContractTerminatedEvent(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "msg-1");
            when(objectMapper.readValue("payload", ContractTerminatedEvent.class)).thenReturn(event);

            consumer.handleContractTerminated(rec, ack);

            verify(houseService).deactivateHouseForUser(event.getTenantId(), event.getHouseId());
            verify(ack).acknowledge();
        }

        @Test
        @DisplayName("rethrows RuntimeException on any exception (for retry)")
        void fails() throws Exception {
            when(objectMapper.readValue(any(String.class), eq(ContractTerminatedEvent.class)))
                    .thenThrow(new RuntimeException("bad"));

            assertThatThrownBy(() -> consumer.handleContractTerminated(rec, ack))
                    .isInstanceOf(RuntimeException.class);
            verify(ack, never()).acknowledge();
        }
    }
}
