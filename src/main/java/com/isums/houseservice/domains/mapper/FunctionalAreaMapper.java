package com.isums.houseservice.domains.mapper;


import com.isums.houseservice.domains.dtos.FunctionalAreaDto.FunctionalAreaDto;
import com.isums.houseservice.domains.entities.FunctionalArea;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FunctionalAreaMapper {
    FunctionalAreaDto mapFunc (FunctionalArea functionalArea);
    List<FunctionalAreaDto> mapFuncs (List<FunctionalArea> functionalAreas);
}
