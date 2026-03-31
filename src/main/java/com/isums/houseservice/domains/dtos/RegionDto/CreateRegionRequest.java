package com.isums.houseservice.domains.dtos.RegionDto;

import com.isums.houseservice.domains.entities.RegionStaff;

import java.util.List;
import java.util.UUID;

public record CreateRegionRequest(
        String name,
        String description,
        UUID managerId,
        List<UUID> technicalStaffIds
) {}