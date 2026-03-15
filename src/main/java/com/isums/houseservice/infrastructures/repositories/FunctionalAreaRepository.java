package com.isums.houseservice.infrastructures.repositories;

import com.isums.houseservice.domains.entities.FunctionalArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FunctionalAreaRepository extends JpaRepository<FunctionalArea, UUID> {
    List<FunctionalArea> findByHouseId(UUID houseId);
}
