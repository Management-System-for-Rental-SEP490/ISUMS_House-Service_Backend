package com.isums.houseservice.infrastructures.mappers;

import com.isums.houseservice.domains.dtos.HouseDto;
import com.isums.houseservice.domains.entities.House;
import com.isums.houseservice.domains.mapper.FunctionalAreaMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {
                FunctionalAreaMapper.class,
                HouseImageMapper.class
        }
)
public interface HouseMapper {
    @Mapping(source = "region.id", target = "regionId")
    @Mapping(target = "functionalAreas", source = "functionalAreas")
    @Mapping(target = "images", source = "houseImages")
    HouseDto toDto(House house);
    List<HouseDto> toDtos(List<House> houses);
}
