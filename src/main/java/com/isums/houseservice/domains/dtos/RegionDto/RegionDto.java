package com.isums.houseservice.domains.dtos.RegionDto;

import java.util.List;
import java.util.UUID;

public record RegionDto (
            UUID id,
            String name,
            String description,
            UUID managerId,
            List<UUID> technicalStaffIds
    ) {}

