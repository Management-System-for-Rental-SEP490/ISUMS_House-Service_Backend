package com.isums.houseservice.domains.dtos.FunctionalAreaDto;

import com.isums.houseservice.domains.emuns.AreaType;
import com.isums.houseservice.domains.emuns.FuctionalAreaStatus;
import com.isums.houseservice.domains.entities.House;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

public record CreateFunctionalAreaRequest(
        UUID house,
        String name,
        AreaType areaType,
        String floorNo,
        String description
) {
}
