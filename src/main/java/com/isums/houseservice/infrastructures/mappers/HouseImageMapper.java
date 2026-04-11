package com.isums.houseservice.infrastructures.mappers;

import com.isums.houseservice.domains.dtos.HouseImageDto;
import com.isums.houseservice.domains.entities.HouseImage;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HouseImageMapper {
    HouseImageDto toDto(HouseImage image);
    List<HouseImageDto> toDtos(List<HouseImage> images);
}
