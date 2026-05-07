package com.isums.houseservice.schedulers;

import com.isums.houseservice.domains.entities.House;
import com.isums.houseservice.infrastructures.abstracts.HouseService;
import com.isums.houseservice.infrastructures.repositories.HouseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantTransitionScheduler")
class TenantTransitionSchedulerTest {

    @Mock private HouseRepository houseRepository;
    @Mock private HouseService houseService;

    @InjectMocks private TenantTransitionScheduler scheduler;

    private House housePending(UUID next, Instant date) {
        return House.builder()
                .id(UUID.randomUUID()).userRentalId(UUID.randomUUID())
                .nextTenantId(next).nextHandoverDate(date).build();
    }

    @Test
    @DisplayName("processHandovers calls activeHouseForUser for each pending house")
    void processesEach() {
        House h1 = housePending(UUID.randomUUID(), Instant.now().minusSeconds(60));
        House h2 = housePending(UUID.randomUUID(), Instant.now().minusSeconds(30));

        when(houseRepository.findPendingHandoversDue(any(Instant.class)))
                .thenReturn(List.of(h1, h2));

        scheduler.processHandovers();

        verify(houseService).activeHouseForUser(h1.getNextTenantId(), h1.getId(), h1.getNextHandoverDate());
        verify(houseService).activeHouseForUser(h2.getNextTenantId(), h2.getId(), h2.getNextHandoverDate());
    }

    @Test
    @DisplayName("does nothing when no pending houses")
    void none() {
        when(houseRepository.findPendingHandoversDue(any(Instant.class)))
                .thenReturn(List.of());

        scheduler.processHandovers();

        verifyNoInteractions(houseService);
    }

    @Test
    @DisplayName("continues processing next house when one fails (error isolation)")
    void isolatesFailures() {
        House ok = housePending(UUID.randomUUID(), Instant.now());
        House bad = housePending(UUID.randomUUID(), Instant.now());

        when(houseRepository.findPendingHandoversDue(any(Instant.class)))
                .thenReturn(List.of(bad, ok));

        doThrow(new RuntimeException("transition broken"))
                .when(houseService).activeHouseForUser(bad.getNextTenantId(), bad.getId(), bad.getNextHandoverDate());

        scheduler.processHandovers();

        verify(houseService).activeHouseForUser(ok.getNextTenantId(), ok.getId(), ok.getNextHandoverDate());
    }
}
