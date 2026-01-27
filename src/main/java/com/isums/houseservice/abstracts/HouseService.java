package com.isums.houseservice.abstracts;

import com.isums.houseservice.domains.dtos.ApiResponse;
import com.isums.houseservice.domains.dtos.CreateHouseRequest;
import com.isums.houseservice.domains.entities.House;

import java.util.List;

public interface HouseService {
    ApiResponse<House> CreateHouse(CreateHouseRequest req);
    ApiResponse<List<House>> GetAllHouses();
}
