package com.isums.houseservice.infrastructures.repositories;

import com.isums.houseservice.domains.entities.House;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HouseRepository extends JpaRepository<House, UUID> {

    @EntityGraph(attributePaths = "functionalAreas")
    Optional<House> findWithFunctionalAreasById(UUID id);
    @Query("SELECT h.region.id FROM House h WHERE h.id = :houseId")
    Optional<UUID> findRegionIdByHouseId(UUID houseId);
    List<House> findByUserRentalId(UUID userRentalId);

    List<House> findByTenantId(UUID tenantId);
}
