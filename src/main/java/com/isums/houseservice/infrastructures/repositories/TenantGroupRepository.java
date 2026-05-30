package com.isums.houseservice.infrastructures.repositories;

import com.isums.houseservice.domains.entities.TenantGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantGroupRepository extends JpaRepository<TenantGroup, UUID> {
    Optional<TenantGroup> findByHouseId(UUID houseId);

    Optional<TenantGroup> findByHouseIdAndIsActiveTrue(UUID houseId);

    List<TenantGroup> findAllByHouseId(UUID houseId);

    List<TenantGroup> findAllByHouseIdAndIsActiveTrue(UUID houseId);
}
