package com.isums.houseservice.domains.dtos;

import com.isums.houseservice.domains.dtos.FunctionalAreaDto.FunctionalAreaDto;
import com.isums.houseservice.domains.emuns.HouseStatus;

import java.util.List;
import java.util.UUID;

public record HouseDto(UUID id,
                       UUID userRentalId,
                       String name, String
                       address,
                       String ward,
                       String commune,
                       String city,
                       String description,
                       HouseStatus status,
                       List<FunctionalAreaDto> functionalAreas) {
}