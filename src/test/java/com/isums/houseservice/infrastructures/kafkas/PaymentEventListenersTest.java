package com.isums.houseservice.infrastructures.kafkas;

import com.isums.houseservice.domains.events.AppAccessChangedEvent;
import com.isums.houseservice.infrastructures.abstracts.HouseService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentEventListeners")
class PaymentEventListenersTest {

    @Mock private HouseService houseService;
    @Mock private ObjectMapper objectMapper;
    @Mock private Acknowledgment ack;

    @InjectMocks private PaymentEventListeners listener;

    private final ConsumerRecord<String, String> rec =
            new ConsumerRecord<>("payment.app-access-changed", 0, 0L, "k", "v");

    @Test
    @DisplayName("sets tenant access restriction and acknowledges on happy path")
    void happyPath() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID houseId = UUID.randomUUID();
        AppAccessChangedEvent event = new AppAccessChangedEvent(
                tenantId, houseId, UUID.randomUUID(), true, "unpaid", "m1");
        when(objectMapper.readValue("v", AppAccessChangedEvent.class)).thenReturn(event);

        listener.handleAppAccessChanged(rec, ack);

        verify(houseService).setTenantAccessRestriction(tenantId, houseId, true);
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("rethrows RuntimeException when deserialization fails (for retry)")
    void deserializeFails() throws Exception {
        when(objectMapper.readValue(any(String.class), eq(AppAccessChangedEvent.class)))
                .thenThrow(new RuntimeException("bad"));

        assertThatThrownBy(() -> listener.handleAppAccessChanged(rec, ack))
                .isInstanceOf(RuntimeException.class);
        verify(ack, never()).acknowledge();
    }
}
