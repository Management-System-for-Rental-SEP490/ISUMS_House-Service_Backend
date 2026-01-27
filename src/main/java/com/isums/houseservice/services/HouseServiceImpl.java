package com.isums.houseservice.services;

import com.isums.houseservice.abstracts.HouseService;
import com.isums.houseservice.domains.dtos.ApiResponse;
import com.isums.houseservice.domains.dtos.ApiResponses;
import com.isums.houseservice.domains.dtos.CreateHouseRequest;
import com.isums.houseservice.domains.emuns.HouseStatus;
import com.isums.houseservice.domains.entities.House;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HouseServiceImpl implements HouseService {

    private final HouseQuery houseQuery;

    @Override
    public ApiResponse<House> CreateHouse(CreateHouseRequest req) {
        try {
            House house = House.builder()
                    .name(req.name())
                    .address(req.address())
                    .ward(req.ward())
                    .commune(req.commune())
                    .city(req.city())
                    .description(req.description())
                    .status(HouseStatus.AVAILABLE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            House created = houseQuery.createHouse(house);

            return ApiResponses.created(created, "Success to create house");
        } catch (Exception ex) {
            return ApiResponses.fail(HttpStatus.INTERNAL_SERVER_ERROR, "Fail to create new house: " + ex.getMessage());
        }
    }

    @Override
    public ApiResponse<List<House>> GetAllHouses() {
        try {

            List<House> houses = houseQuery.GetAllHouses();

            return ApiResponses.created(houses, "Success to get all houses");
        } catch (Exception ex) {
            return ApiResponses.fail(HttpStatus.INTERNAL_SERVER_ERROR, "Fail to get all houses: " + ex.getMessage());
        }
    }


}
