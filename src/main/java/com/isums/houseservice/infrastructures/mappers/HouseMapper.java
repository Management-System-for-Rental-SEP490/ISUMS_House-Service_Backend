package com.isums.houseservice.infrastructures.mappers;

import com.isums.houseservice.domains.dtos.HouseDto;
import com.isums.houseservice.domains.entities.House;
import com.isums.houseservice.domains.mapper.FunctionalAreaMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring",uses = FunctionalAreaMapper.class)
public interface HouseMapper {
    @Mapping(target = "functionalAreas", source = "functionalAreas")
    HouseDto toDto(House house);
    List<HouseDto> toDtos(List<House> houses);
}
