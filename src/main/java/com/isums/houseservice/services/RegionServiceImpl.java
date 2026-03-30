package com.isums.houseservice.services;

import com.isums.houseservice.domains.dtos.RegionDto.CreateRegionRequest;
import com.isums.houseservice.domains.dtos.RegionDto.RegionDto;
import com.isums.houseservice.domains.dtos.RegionDto.UpdateRegionRequest;
import com.isums.houseservice.domains.entities.Region;
import com.isums.houseservice.domains.entities.RegionStaff;
import com.isums.houseservice.domains.entities.RegionStaffId;
import com.isums.houseservice.infrastructures.abstracts.RegionService;
import com.isums.houseservice.infrastructures.mappers.RegionMapper;
import com.isums.houseservice.infrastructures.repositories.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
                    .build();

            Region savedRegion = regionRepository.save(region);

            if (request.technicalStaffIds() != null && !request.technicalStaffIds().isEmpty()) {

                List<RegionStaff> staffs = request.technicalStaffIds().stream()
                        .map(staffId -> RegionStaff.builder()
                                .id(new RegionStaffId(savedRegion.getId(), staffId))
                                .region(savedRegion)
                                .build()
                        )
                        .toList();

                savedRegion.setStaffs(staffs);
            }

            Region created = regionRepository.save(savedRegion);

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
                region.setName(request.name());
            }
            if(request.description() != null){
                region.setDescription(request.description());
            }

            if (request.technicalStaffIds() != null) {

                Set<UUID> currentStaffIds = region.getStaffs().stream()
                        .map(s -> s.getId().getStaffId())
                        .collect(Collectors.toSet());

                Set<UUID> newStaffIds = new HashSet<>(request.technicalStaffIds());

                region.getStaffs().removeIf(s ->
                        !newStaffIds.contains(s.getId().getStaffId())
                );

                for (UUID staffId : newStaffIds) {
                    if (!currentStaffIds.contains(staffId)) {
                        RegionStaff rs = RegionStaff.builder()
                                .id(new RegionStaffId(region.getId(), staffId))
                                .region(region)
                                .build();

                        region.getStaffs().add(rs);
                    }
                }
            }

                Region updated = regionRepository.save(region);

                return regionMapper.mapRegion(updated);

        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset item: " + ex.getMessage());
        }
    }
}
