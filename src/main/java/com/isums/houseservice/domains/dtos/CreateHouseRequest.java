package com.isums.houseservice.domains.dtos;

import com.isums.houseservice.domains.emuns.HouseStatus;

import java.util.UUID;

public record CreateHouseRequest(
    String name,
    String address,
    String ward,
    String commune,
    String city,
    String description
) {
}
