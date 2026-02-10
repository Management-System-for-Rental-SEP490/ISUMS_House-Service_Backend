package com.isums.houseservice.abstracts;

import com.isums.houseservice.domains.dtos.FunctionalAreaDto.CreateFunctionalAreaRequest;
import com.isums.houseservice.domains.dtos.FunctionalAreaDto.FunctionalAreaDto;
import com.isums.houseservice.domains.dtos.FunctionalAreaDto.UpdateFunctionalAreaRequest;
import com.isums.houseservice.infrastructures.repositories.FunctionalAreaRepository;

import java.util.List;
import java.util.UUID;

public interface FunctionalAreaService {
    FunctionalAreaDto createArea(CreateFunctionalAreaRequest request);
    List<FunctionalAreaDto> getAllAreas();
    FunctionalAreaDto updateArea(UUID id, UpdateFunctionalAreaRequest request);
    Boolean deleteArea(UUID id);
}
