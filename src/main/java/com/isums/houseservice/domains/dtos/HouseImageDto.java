package com.isums.houseservice.domains.dtos;

import java.time.Instant;
import java.util.UUID;

public record HouseImageDto(
        UUID id,
        String url,
        Instant createdAt
) {
}
