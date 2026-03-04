package com.isums.houseservice.services;

import com.isums.houseservice.domains.dtos.RegionDto.CreateRegionRequest;
import com.isums.houseservice.domains.dtos.RegionDto.RegionDto;
import com.isums.houseservice.domains.dtos.RegionDto.UpdateRegionRequest;
import com.isums.houseservice.domains.entities.Region;
import com.isums.houseservice.infrastructures.abstracts.RegionService;
import com.isums.houseservice.infrastructures.mappers.RegionMapper;
import com.isums.houseservice.infrastructures.repositories.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionService {
    private final RegionRepository regionRepository;
    private final RegionMapper regionMapper;

    @Override
    public RegionDto createRegion(CreateRegionRequest request) {
        try{
            Region region = Region.builder()
                    .name(request.name())
                    .description(request.description())
                    .managerId(request.managerId())
                    .technicalStaffIds(request.technicalStaffIds())
                    .build();
            Region created = regionRepository.save(region);

            return regionMapper.mapRegion(created);
        } catch (Exception ex) {
            throw new RuntimeException("Error to create region : " + ex.getMessage());
        }
    }

    @Transactional
    @Override
    public List<RegionDto> getAllRegions() {
        try{
            List<Region> regions = regionRepository.findAll();
            return regionMapper.mapRegions(regions);
        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset item: " + ex.getMessage());
        }
    }

    @Override
    public RegionDto getById(UUID id) {
        try{

            Region region = regionRepository.findById(id)
                    .orElseThrow(()-> new RuntimeException("Can't not find region"));

            return regionMapper.mapRegion(region);
        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset item: " + ex.getMessage());
        }
    }

    @Override
    public RegionDto updateRegion(UUID id, UpdateRegionRequest request) {
        try{
            Region region = regionRepository.findById(id)
                    .orElseThrow(()-> new RuntimeException("Id not found"));

            if(request.name() != null){
                region.setName(region.getName());
            }
            if(request.description() != null){
                region.setDescription(region.getDescription());
            }

            Region updated = regionRepository.save(region);

            return regionMapper.mapRegion(updated);

        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset item: " + ex.getMessage());
        }
    }
}
