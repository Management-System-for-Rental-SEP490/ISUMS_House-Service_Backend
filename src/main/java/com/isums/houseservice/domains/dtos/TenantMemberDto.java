package com.isums.houseservice.domains.dtos;

import com.isums.houseservice.domains.emuns.HouseMemberRole;

import java.time.Instant;
import java.util.UUID;

public record TenantMemberDto(
        UUID userId,
        HouseMemberRole role,
        boolean isActive,
        Instant joinedAt
) {
}