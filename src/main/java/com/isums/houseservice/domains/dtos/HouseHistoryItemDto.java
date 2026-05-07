package com.isums.houseservice.domains.dtos;

import java.time.Instant;
import java.util.UUID;

public record HouseHistoryItemDto(
        UUID id,
        String source,
        String type,
        String status,
        String title,
        String description,
        UUID assetId,
        UUID slotId,
        UUID assignedStaffId,
        String staffName,
        String staffPhone,
        Instant createdAt,
        Instant happenedAt
) {
}
