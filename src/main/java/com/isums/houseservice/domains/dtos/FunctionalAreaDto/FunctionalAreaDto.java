package com.isums.houseservice.domains.dtos.FunctionalAreaDto;

import com.isums.houseservice.domains.emuns.AreaType;
import com.isums.houseservice.domains.emuns.FuctionalAreaStatus;

import java.time.Instant;
import java.util.UUID;

public record FunctionalAreaDto(
    UUID id,
    UUID houseId,
    String name,
    AreaType areaType,
    String floorNo,
    String description,
    FuctionalAreaStatus status,
    Instant createdAt,
    Instant updatedAt
){
}
