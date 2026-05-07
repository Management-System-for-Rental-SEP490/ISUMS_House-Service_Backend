package com.isums.houseservice.infrastructures.repositories;

import com.isums.houseservice.domains.entities.HouseSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HouseSubscriptionRepository extends JpaRepository<HouseSubscription, UUID> {

    Optional<HouseSubscription> findByUserIdAndHouseId(UUID userId, UUID houseId);

    List<HouseSubscription> findByHouseIdAndNotifiedAtIsNull(UUID houseId);

    List<HouseSubscription> findByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndHouseId(UUID userId, UUID houseId);

    @Modifying
    @Query("DELETE FROM HouseSubscription s WHERE s.userId = :userId AND s.houseId = :houseId")
    int deleteByUserIdAndHouseId(@Param("userId") UUID userId, @Param("houseId") UUID houseId);
}
