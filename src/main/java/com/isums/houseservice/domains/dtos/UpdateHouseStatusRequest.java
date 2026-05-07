package com.isums.houseservice.domains.dtos;

import com.isums.houseservice.domains.emuns.HouseStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateHouseStatusRequest(@NotNull HouseStatus status) {}
