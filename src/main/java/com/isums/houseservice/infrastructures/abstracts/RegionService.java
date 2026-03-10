package com.isums.houseservice.infrastructures.abstracts;

import com.isums.houseservice.domains.dtos.RegionDto.CreateRegionRequest;
import com.isums.houseservice.domains.dtos.RegionDto.RegionDto;
import com.isums.houseservice.domains.dtos.RegionDto.UpdateRegionRequest;

import java.util.List;
import java.util.UUID;

public interface RegionService {
    RegionDto createRegion(CreateRegionRequest request);
    List<RegionDto> getAllRegions();
    RegionDto getById(UUID id);
    RegionDto updateRegion(UUID id, UpdateRegionRequest request);
}
