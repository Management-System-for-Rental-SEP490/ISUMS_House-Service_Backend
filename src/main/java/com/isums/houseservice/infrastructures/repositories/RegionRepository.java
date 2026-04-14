package com.isums.houseservice.infrastructures.repositories;

import com.isums.houseservice.domains.entities.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegionRepository extends JpaRepository<Region, UUID> {
    Optional<Region> findByManagerId(UUID managerId);
    List<Region> findAllByManagerId(UUID managerId);
}
