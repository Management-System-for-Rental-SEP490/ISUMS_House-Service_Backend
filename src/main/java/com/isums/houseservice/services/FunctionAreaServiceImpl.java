package com.isums.houseservice.services;

import com.isums.houseservice.infrastructures.abstracts.FunctionalAreaService;
import com.isums.houseservice.domains.dtos.FunctionalAreaDto.CreateFunctionalAreaRequest;
import com.isums.houseservice.domains.dtos.FunctionalAreaDto.FunctionalAreaDto;
import com.isums.houseservice.domains.dtos.FunctionalAreaDto.UpdateFunctionalAreaRequest;
import com.isums.houseservice.domains.emuns.FuctionalAreaStatus;
import com.isums.houseservice.domains.entities.FunctionalArea;
import com.isums.houseservice.domains.entities.House;
import com.isums.houseservice.domains.mapper.FunctionalAreaMapper;
import com.isums.houseservice.infrastructures.repositories.FunctionalAreaRepository;
import com.isums.houseservice.infrastructures.repositories.HouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FunctionAreaServiceImpl implements FunctionalAreaService {
    private final HouseRepository houseRepository;
    private final FunctionalAreaMapper functionalAreaMapper;
    private final FunctionalAreaRepository functionalAreaRepository;

    @Override
    public FunctionalAreaDto createArea(CreateFunctionalAreaRequest request) {
        try{
            House house = houseRepository.findById(request.house())
                    .orElseThrow(() -> new RuntimeException("Id not found"));

            FunctionalArea functionalArea =FunctionalArea.builder()
                    .house(house)
                    .name(request.name())
                    .areaType(request.areaType())
                    .floorNo(request.floorNo())
                    .description(request.description())
                    .status(FuctionalAreaStatus.NORMAL)
                    .createdAt(Instant.now())
                    .build();

            FunctionalArea created = functionalAreaRepository.save(functionalArea);

            return functionalAreaMapper.mapFunc(created);

        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset item: " + ex.getMessage());        }
        }

    @Override
    public List<FunctionalAreaDto> getAllAreas() {
        try{
            List<FunctionalArea> mapAreas = functionalAreaRepository.findAll();
            return functionalAreaMapper.mapFuncs(mapAreas);
        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset item: " + ex.getMessage());
        }
    }

    @Override
    public FunctionalAreaDto updateArea(UUID id , UpdateFunctionalAreaRequest request) {
        try{
            FunctionalArea area = functionalAreaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Id not found"));

            if(request.name() != null){
                area.setName(request.name());
            }
            if(request.areaType() != null){
                area.setAreaType(request.areaType());
            }
            if(request.floorNo() != null){
                area.setFloorNo(request.floorNo());
            }
            if(request.description() != null){
                area.setDescription(request.description());
            }
            if(request.status() != null){
                area.setStatus(request.status());
            }

            FunctionalArea update = functionalAreaRepository.save(area);
            return functionalAreaMapper.mapFunc(update);

        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset item: " + ex.getMessage());
        }


    }

    @Override
    public Boolean deleteArea(UUID id) {
        try{
            FunctionalArea area = functionalAreaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Id not found"));

            area.setStatus(FuctionalAreaStatus.UNAVAILABLE);
            functionalAreaRepository.save(area);

            return true;
        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset item: " + ex.getMessage());
        }
    }
}



