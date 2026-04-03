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
import com.isums.houseservice.infrastructures.repositories.RegionStaffRepository;
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
    private final RegionStaffRepository regionStaffRepository;

    @Override
    public RegionDto createRegion(CreateRegionRequest request) {
        try {
            Region region = Region.builder()
                    .name(request.name())
                    .description(request.description())
                    .managerId(request.managerId())
                    .build();

            regionRepository.save(region);

            if (request.technicalStaffIds() != null && !request.technicalStaffIds().isEmpty()) {

                List<RegionStaff> staffs = request.technicalStaffIds().stream()
                        .distinct()
                        .map(staffId -> RegionStaff.builder()
                                .id(new RegionStaffId(region.getId(), staffId))
                                .region(region)
                                .build()
                        )
                        .toList();

                regionStaffRepository.saveAll(staffs);
            }

            return regionMapper.mapRegion(region);

        } catch (Exception ex) {
            throw new RuntimeException("Error to create region: " + ex.getMessage());
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
                    .orElseThrow(()-> new RuntimeException("Region not found"));

            if(request.name() != null){
                region.setName(request.name());
            }
            if(request.description() != null){
                region.setDescription(request.description());
            }

            if (request.technicalStaffIds() != null) {

                List<RegionStaff> current = regionStaffRepository.findByIdRegionId(id);

                Set<UUID> currentIds = current.stream()
                        .map(rs -> rs.getId().getStaffId())
                        .collect(Collectors.toSet());

                Set<UUID> newIds = new HashSet<>(request.technicalStaffIds());

                List<RegionStaff> toRemove = current.stream()
                        .filter(rs -> !newIds.contains(rs.getId().getStaffId()))
                        .toList();

                regionStaffRepository.deleteAll(toRemove);

                List<RegionStaff> toAdd = newIds.stream()
                        .filter(staffId -> !currentIds.contains(staffId))
                        .map(staffId -> RegionStaff.builder()
                                .id(new RegionStaffId(id, staffId))
                                .region(region)
                                .build())
                        .toList();

                regionStaffRepository.saveAll(toAdd);
            }


            Region updated = regionRepository.save(region);

                return regionMapper.mapRegion(updated);

        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset item: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    public RegionDto addStaffToRegion(UUID regionId, UUID staffId) {

        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new RuntimeException("Region not found"));

        RegionStaffId id = new RegionStaffId(regionId, staffId);

        if (!regionStaffRepository.existsById(id)) {
            RegionStaff rs = RegionStaff.builder()
                    .id(id)
                    .region(region)
                    .build();

            regionStaffRepository.save(rs);
        }

        // 🔥 lấy lại staffIds
        List<UUID> staffIds = regionStaffRepository.findByIdRegionId(regionId)
                .stream()
                .map(s -> s.getId().getStaffId())
                .toList();

        return regionMapper.mapRegion(region, staffIds);
    }

    @Override
    @Transactional
    public RegionDto removeStaffFromRegion(UUID regionId, UUID staffId) {

        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new RuntimeException("Region not found"));

        RegionStaffId id = new RegionStaffId(regionId, staffId);

        regionStaffRepository.deleteById(id);

        List<UUID> staffIds = regionStaffRepository.findByIdRegionId(regionId)
                .stream()
                .map(s -> s.getId().getStaffId())
                .toList();

        return regionMapper.mapRegion(region, staffIds);
    }
}
