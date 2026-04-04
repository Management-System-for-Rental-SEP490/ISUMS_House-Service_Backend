package com.isums.houseservice.infrastructures.repositories;

import com.isums.houseservice.domains.entities.RegionStaff;
import com.isums.houseservice.domains.entities.RegionStaffId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface RegionStaffRepository extends JpaRepository<RegionStaff, RegionStaffId> {
    List<RegionStaff> findByIdRegionId(UUID regionId);
    @Query("SELECT rs.id.staffId FROM RegionStaff rs WHERE rs.id.regionId = :regionId")
    List<UUID> findStaffIdsByRegionId(UUID regionId);
}
