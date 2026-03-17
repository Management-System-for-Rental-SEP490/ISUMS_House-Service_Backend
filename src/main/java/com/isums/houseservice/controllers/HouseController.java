package com.isums.houseservice.controllers;

import com.isums.houseservice.domains.dtos.HouseDto;
import com.isums.houseservice.infrastructures.abstracts.HouseService;
import com.isums.houseservice.domains.dtos.ApiResponse;
import com.isums.houseservice.domains.dtos.ApiResponses;
import com.isums.houseservice.domains.dtos.CreateHouseRequest;
import com.isums.houseservice.domains.entities.House;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/houses")
@RequiredArgsConstructor
public class HouseController {
    private final HouseService houseService;

    @GetMapping
    public ApiResponse<List<HouseDto>> GetAllHouses() {
        List<HouseDto> res = houseService.GetAllHouses();
        return ApiResponses.ok(res, "Success to get all houses");
    }

    @PostMapping
    public ApiResponse<HouseDto> CreateHouse(@RequestBody CreateHouseRequest req) {
        HouseDto res = houseService.CreateHouse(req);
        return ApiResponses.created(res, "Success to create house");
    }

    @GetMapping("/{id}")
    public ApiResponse<HouseDto> getHouseById(@PathVariable UUID id) {
        HouseDto house = houseService.getHouseById(id);
        return ApiResponses.ok(house, "Success to get house by id");
    }

    @GetMapping("/house")
    public ApiResponse<List<HouseDto>> getMyHouses(@AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());

        List<HouseDto> houses = houseService.getHousesByUser(userId);

        return ApiResponses.ok(houses, "Get houses successfully");
    }
}
