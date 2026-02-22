package com.isums.houseservice.infrastructures.mappers;

import com.isums.houseservice.domains.dtos.HouseDto;
import com.isums.houseservice.domains.entities.House;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HouseMapper {
    HouseDto toDto(House house);
    List<HouseDto> toDtos(List<House> houses);
}
