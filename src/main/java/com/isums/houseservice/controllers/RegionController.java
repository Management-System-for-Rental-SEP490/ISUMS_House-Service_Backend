package com.isums.houseservice.controllers;


import com.isums.houseservice.domains.dtos.ApiResponse;
import com.isums.houseservice.domains.dtos.ApiResponses;
import com.isums.houseservice.domains.dtos.RegionDto.CreateRegionRequest;
import com.isums.houseservice.domains.dtos.RegionDto.RegionDto;
import com.isums.houseservice.domains.dtos.RegionDto.UpdateRegionRequest;
import com.isums.houseservice.infrastructures.abstracts.RegionService;
import com.isums.houseservice.infrastructures.grpcs.UserClientsGrpc;
import com.isums.userservice.grpc.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/houses/regions")
@RequiredArgsConstructor
public class RegionController {
    private final RegionService regionService;
    private final UserClientsGrpc userClientsGrpc;

    @PostMapping
    public ApiResponse<RegionDto> createRegion(@AuthenticationPrincipal Jwt jwt, @RequestBody CreateRegionRequest request){
        UserResponse user = userClientsGrpc.getUserIdAndRoleByKeyCloakId(jwt.getSubject());
        RegionDto response = regionService.createRegion(user.getId(),request);
        return ApiResponses.created(response,"Create region successfully");
    }

    @PostMapping("/{regionId}/staff/{staffId}")
    public ApiResponse<RegionDto> addStaff(@PathVariable UUID regionId, @PathVariable UUID staffId) {
        RegionDto res = regionService.addStaffToRegion(regionId, staffId);
        return ApiResponses.ok(res,"Add staff to region successfully");
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

//    @PutMapping("/{regionId}")
//    public ApiResponse<RegionDto> updateRegion(@PathVariable UUID regionId, @RequestBody UpdateRegionRequest request) {
//        RegionDto response = regionService.updateRegion(regionId,request);
//        return ApiResponses.ok(response,"Update region successfully");
//    }

    @DeleteMapping("/{regionId}/staff/{staffId}")
    public ApiResponse<RegionDto> removeStaff(@PathVariable UUID regionId, @PathVariable UUID staffId) {
        RegionDto res = regionService.removeStaffFromRegion(regionId, staffId);
        return ApiResponses.ok(res,"Remove staff from region successfully");
    }

    @GetMapping("staff/{staffId}")
    public ApiResponse<List<RegionDto>> getMyRegion(@PathVariable UUID staffId) {
        List<RegionDto> res = regionService.getRegionByStaffId(staffId);
        return ApiResponses.ok(res,"Get region by staff successfully");
    }
}
