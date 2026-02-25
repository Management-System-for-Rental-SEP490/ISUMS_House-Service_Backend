package com.isums.houseservice.infrastructures.abstracts;

import com.isums.houseservice.domains.dtos.FunctionalAreaDto.CreateFunctionalAreaRequest;
import com.isums.houseservice.domains.dtos.FunctionalAreaDto.FunctionalAreaDto;
import com.isums.houseservice.domains.dtos.FunctionalAreaDto.UpdateFunctionalAreaRequest;

import java.util.List;
import java.util.UUID;

public interface FunctionalAreaService {
    FunctionalAreaDto createArea(CreateFunctionalAreaRequest request);
    List<FunctionalAreaDto> getAllAreas();
    FunctionalAreaDto updateArea(UUID id, UpdateFunctionalAreaRequest request);
    Boolean deleteArea(UUID id);
}
