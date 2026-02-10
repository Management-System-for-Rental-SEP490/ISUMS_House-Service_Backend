package com.isums.houseservice.domains.dtos.FunctionalAreaDto;

import com.isums.houseservice.domains.emuns.AreaType;
import com.isums.houseservice.domains.emuns.FuctionalAreaStatus;

import java.time.Instant;

public record UpdateFunctionalAreaRequest(
        String name,
        AreaType areaType,
        String floorNo,
        String description,
        FuctionalAreaStatus status,
        Instant updateAt
) {
}
