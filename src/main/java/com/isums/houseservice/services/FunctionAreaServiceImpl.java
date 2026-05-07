package com.isums.houseservice.services;

import com.isums.houseservice.infrastructures.abstracts.FunctionalAreaService;
import com.isums.houseservice.domains.dtos.FunctionalAreaDto.CreateFunctionalAreaRequest;
import com.isums.houseservice.domains.dtos.FunctionalAreaDto.FunctionalAreaDto;
import com.isums.houseservice.domains.dtos.FunctionalAreaDto.UpdateFunctionalAreaRequest;
import com.isums.houseservice.domains.emuns.FuctionalAreaStatus;
import com.isums.houseservice.domains.entities.FunctionalArea;
import com.isums.houseservice.domains.entities.House;
import com.isums.houseservice.domains.mapper.FunctionalAreaMapper;
import com.isums.houseservice.exceptions.NotFoundException;
import com.isums.houseservice.infrastructures.repositories.FunctionalAreaRepository;
import com.isums.houseservice.infrastructures.repositories.HouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FunctionAreaServiceImpl implements FunctionalAreaService {
    private final HouseRepository houseRepository;
    private final FunctionalAreaMapper functionalAreaMapper;
    private final FunctionalAreaRepository functionalAreaRepository;
    private final TranslationAutoFillService translationAutoFillService;

    @Override
    @Transactional
    public FunctionalAreaDto createArea(CreateFunctionalAreaRequest request) {
        House house = houseRepository.findById(request.house())
                .orElseThrow(() -> new NotFoundException("House not found: " + request.house()));

        FunctionalArea functionalArea = FunctionalArea.builder()
                .house(house)
                .name(request.name())
                .nameTranslations(translationAutoFillService.complete(request.name()))
                .areaType(request.areaType())
                .floorNo(request.floorNo())
                .description(request.description())
                .descriptionTranslations(translationAutoFillService.complete(request.description()))
                .status(FuctionalAreaStatus.NORMAL)
                .createdAt(Instant.now())
                .build();

        FunctionalArea created = functionalAreaRepository.save(functionalArea);

        return functionalAreaMapper.mapFunc(created);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FunctionalAreaDto> getAllAreas(UUID houseId) {
        List<FunctionalArea> areas = functionalAreaRepository.findByHouseId(houseId);
        return functionalAreaMapper.mapFuncs(areas);
    }

    @Override
    @Transactional
    public FunctionalAreaDto updateArea(UUID id, UpdateFunctionalAreaRequest request) {
        FunctionalArea area = functionalAreaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Functional area not found: " + id));

        if (request.name() != null) {
            area.setName(request.name());
            area.setNameTranslations(translationAutoFillService.complete(request.name()));
        }
        if (request.areaType() != null) {
            area.setAreaType(request.areaType());
        }
        if (request.floorNo() != null) {
            area.setFloorNo(request.floorNo());
        }
        if (request.description() != null) {
            area.setDescription(request.description());
            area.setDescriptionTranslations(translationAutoFillService.complete(request.description()));
        }
        if (request.status() != null) {
            area.setStatus(request.status());
        }

        FunctionalArea updated = functionalAreaRepository.save(area);
        return functionalAreaMapper.mapFunc(updated);
    }

    @Override
    @Transactional
    public Boolean deleteArea(UUID id) {
        FunctionalArea area = functionalAreaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Functional area not found: " + id));

        area.setStatus(FuctionalAreaStatus.UNAVAILABLE);
        functionalAreaRepository.save(area);

        return true;
    }
}
