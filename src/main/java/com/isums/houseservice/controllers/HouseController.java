package com.isums.houseservice.controllers;

import com.isums.houseservice.abstracts.HouseService;
import com.isums.houseservice.domains.dtos.ApiResponse;
import com.isums.houseservice.domains.dtos.CreateHouseRequest;
import com.isums.houseservice.domains.entities.House;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/houses")
@RequiredArgsConstructor
public class HouseController {
    private final HouseService houseService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<House>>> GetAllHouses() {
        ApiResponse<List<House>> res = houseService.GetAllHouses();
        return ResponseEntity.status(res.getStatusCode()).body(res);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<House>> CreateHouse(@RequestBody CreateHouseRequest req) {
        ApiResponse<House> res = houseService.CreateHouse(req);
        return ResponseEntity.status(res.getStatusCode()).body(res);
    }
}
