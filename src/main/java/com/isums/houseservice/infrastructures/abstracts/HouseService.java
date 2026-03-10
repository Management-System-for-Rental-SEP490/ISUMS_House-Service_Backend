package com.isums.houseservice.infrastructures.abstracts;

import com.isums.houseservice.domains.dtos.CreateHouseRequest;
import com.isums.houseservice.domains.dtos.HouseDto;
import com.isums.houseservice.domains.entities.House;

import java.util.List;
import java.util.UUID;

public interface HouseService {
    HouseDto CreateHouse(CreateHouseRequest req);
    List<HouseDto> GetAllHouses();
    House getHouseById(UUID id);
    List<HouseDto> getHousesByUser(UUID userId);
}
