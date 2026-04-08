package com.isums.houseservice.schedulers;

import com.isums.houseservice.domains.entities.House;
import com.isums.houseservice.infrastructures.abstracts.HouseService;
import com.isums.houseservice.infrastructures.repositories.HouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantTransitionScheduler {

    private final HouseRepository houseRepository;
    private final HouseService houseService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void processHandovers() {
        Instant now = Instant.now();

        List<House> pending = houseRepository.findByNextTenantIdIsNotNullAndNextHandoverDateBefore(now);

        for (House house : pending) {
            try {
                log.info("[Scheduler] Transitioning house={} from={} to={}",
                        house.getId(), house.getUserRentalId(), house.getNextTenantId());

                houseService.activeHouseForUser(
                        house.getNextTenantId(),
                        house.getId(),
                        house.getNextHandoverDate()
                );
            } catch (Exception e) {
                log.error("[Scheduler] Transition failed houseId={}: {}",
                        house.getId(), e.getMessage(), e);
            }
        }

        log.info("[Scheduler] Processed {} handovers", pending.size());
    }
}