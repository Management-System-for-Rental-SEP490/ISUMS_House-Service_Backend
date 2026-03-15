package com.isums.houseservice.controllers;


import com.isums.houseservice.domains.dtos.ApiResponse;
import com.isums.houseservice.domains.dtos.ApiResponses;
import com.isums.houseservice.domains.dtos.RegionDto.CreateRegionRequest;
import com.isums.houseservice.domains.dtos.RegionDto.RegionDto;
import com.isums.houseservice.infrastructures.abstracts.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/houses/regions")
@RequiredArgsConstructor
public class RegionController {
    private final RegionService regionService;

    @PostMapping
    public ApiResponse<RegionDto> createRegion(@RequestBody CreateRegionRequest request){
        RegionDto response = regionService.createRegion(request);
        return ApiResponses.created(response,"Create region successfully");
    }

    @GetMapping
    public ApiResponse<List<RegionDto>> getAllRegions(){
        List<RegionDto> response = regionService.getAllRegions();
        return ApiResponses.ok(response,"Get all regions successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<RegionDto> getById(@PathVariable UUID id){
        RegionDto response = regionService.getById(id);
        return ApiResponses.ok(response,"Get region successfully");
    }
}
