package com.isums.houseservice.domains.dtos;

import com.isums.houseservice.domains.emuns.HouseStatus;

import java.util.List;
import java.util.UUID;

public record CreateHouseRequest(
    String name,
    String address,
    UUID regionId,
    String ward,
    String commune,
    String city,
    String description,
    List<String> houseImages
) {
}
