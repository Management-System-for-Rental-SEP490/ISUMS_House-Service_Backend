package com.isums.houseservice.infrastructures.mappers;

import com.isums.houseservice.domains.dtos.RegionDto.RegionDto;
import com.isums.houseservice.domains.entities.Region;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RegionMapper {
    RegionDto mapRegion (Region region);
    List<RegionDto> mapRegions (List<Region> regions);

}
