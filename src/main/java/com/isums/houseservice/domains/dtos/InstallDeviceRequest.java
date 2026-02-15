package com.isums.houseservice.domains.dtos;

import com.isums.houseservice.domains.emuns.DeviceType;

import java.util.UUID;

public record InstallDeviceRequest(
        UUID houseId,
        UUID assetId,
        String thingName,
        DeviceType type
) {}