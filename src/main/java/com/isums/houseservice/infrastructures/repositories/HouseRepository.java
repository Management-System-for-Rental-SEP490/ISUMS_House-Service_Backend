package com.isums.houseservice.infrastructures.repositories;

import com.isums.houseservice.domains.entities.House;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HouseRepository extends JpaRepository<House, UUID> {

    @EntityGraph(attributePaths = "functionalAreas")
    Optional<House> findWithFunctionalAreasById(UUID id);

    @Query("SELECT h.region.id FROM House h WHERE h.id = :houseId")
    Optional<UUID> findRegionIdByHouseId(UUID houseId);

    @EntityGraph(attributePaths = {"functionalAreas", "region"})
    List<House> findByUserRentalId(UUID userRentalId);

    @Query("""
                SELECT h FROM House h
                JOIN TenantGroup g ON g.houseId = h.id
                JOIN TenantMember m ON m.id.tenantId = g.id
                WHERE m.id.userId = :userId
                AND m.isActive = true
                AND g.isActive = true
            """)
    List<House> findByTenantGroupMemberUserId(UUID userId);

    List<House> findByNextTenantIdIsNotNullAndNextHandoverDateBefore(Instant now);

    @Query("""
    SELECT h FROM House h
    WHERE h.nextTenantId = :userId
    OR EXISTS (
        SELECT 1 FROM TenantGroup g
        JOIN TenantMember m ON m.id.tenantId = g.id
        WHERE g.houseId = h.id
        AND m.id.userId = :userId
        AND m.isActive = true
        AND g.isActive = true
    )
""")
    List<House> findAccessibleByUserId(UUID userId);

    @Query("SELECT h FROM House h WHERE h.region.id = :regionId")
    List<House> findAllByRegionId(UUID regionId);
}
