package com.isums.houseservice.domains.dtos;

import com.isums.houseservice.domains.emuns.DeviceType;

import java.util.UUID;

public record ThingLookupResponse(
        String thingName,
        UUID houseId,
        UUID assetId,
        DeviceType type,
        boolean active,
        String healthStatus
) {
}
