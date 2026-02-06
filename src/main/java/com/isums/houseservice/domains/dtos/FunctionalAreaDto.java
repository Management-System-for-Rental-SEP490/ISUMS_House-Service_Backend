package com.isums.houseservice.domains.dtos;

import com.isums.houseservice.domains.emuns.AreaType;
import com.isums.houseservice.domains.emuns.FuctionalAreaStatus;

import java.util.UUID;

public record FunctionalAreaDto(UUID id,
                                String name,
                                AreaType areaType,
                                String floorNo,
                                String description,
                                FuctionalAreaStatus status) {
}
