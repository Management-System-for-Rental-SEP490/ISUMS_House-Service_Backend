package com.isums.houseservice.domains.dtos;

import java.time.Instant;
import java.util.UUID;

public record TenantDto(
        UUID id,
        Boolean isActive,
        Instant createdAt
) {
}
