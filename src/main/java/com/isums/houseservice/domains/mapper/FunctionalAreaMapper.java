package com.isums.houseservice.domains.mapper;


import com.isums.houseservice.domains.dtos.FunctionalAreaDto.FunctionalAreaDto;
import com.isums.houseservice.domains.entities.FunctionalArea;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FunctionalAreaMapper {
    @Mapping(source = "house.id",target = "houseId")
    FunctionalAreaDto mapFunc (FunctionalArea functionalArea);
    List<FunctionalAreaDto> mapFuncs (List<FunctionalArea> functionalAreas);
}
