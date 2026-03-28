package com.isums.houseservice.infrastructures.repositories;

import com.isums.houseservice.domains.entities.RegionStaff;
import com.isums.houseservice.domains.entities.RegionStaffId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegionStaffRepository extends JpaRepository<RegionStaff, RegionStaffId> {
    List<RegionStaff> findByIdRegionId(UUID regionId);
    List<RegionStaff> findByIdStaffId(UUID staffId);
}
