package com.isums.houseservice.infrastructures.mappers;

import com.isums.houseservice.domains.dtos.RegionDto.RegionDto;
import com.isums.houseservice.domains.entities.Region;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface RegionMapper {

    @Mapping(target = "staffIds", source = "staffIds")
    RegionDto toDto(Region region, List<UUID> staffIds);

    List<RegionDto> toDtos(List<Region> regions);

}
