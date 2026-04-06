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
    public RegionDto createRegion(String managerId,CreateRegionRequest request) {
        try {
            Region region = Region.builder()
                    .name(request.name())
                    .description(request.description())
                    .managerId(UUID.fromString(managerId))
                    .build();

            regionRepository.save(region);

            List<UUID> staffIds = List.of();

            if (request.technicalStaffIds() != null && !request.technicalStaffIds().isEmpty()) {

                staffIds = request.technicalStaffIds().stream()
                        .distinct()
                        .toList();

                List<RegionStaff> staffs = staffIds.stream()
                        .map(staffId -> RegionStaff.builder()
                                .id(new RegionStaffId(region.getId(), staffId))
                                .region(region)
                                .build())
                        .toList();

                regionStaffRepository.saveAll(staffs);
            }

            return regionMapper.toDto(region, staffIds);

        } catch (Exception ex) {
            throw new RuntimeException("Error to create region: " + ex.getMessage());
        }
    }

    @Transactional
    @Override
    public List<RegionDto> getAllRegions() {
        try{
            List<Region> regions = regionRepository.findAll();

            return regions.stream().map(region -> {
                List<UUID> staffIds = regionStaffRepository.findStaffIdsByRegionId(region.getId());
                return regionMapper.toDto(region, staffIds);
            }).toList();
        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset item: " + ex.getMessage());
        }
    }

    @Override
    public RegionDto getById(UUID id) {
        try{
            Region region = regionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Region not found"));
            List<UUID> staffIds = regionStaffRepository.findStaffIdsByRegionId(id);

            return regionMapper.toDto(region, staffIds);
        } catch (Exception ex) {
            throw new RuntimeException("Error to get asset item: " + ex.getMessage());
        }
    }

    @Override
    public RegionDto updateRegion(UUID id, UpdateRegionRequest request) {
        try{
            Region region = regionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Region not found"));

            if (request.name() != null) {
                region.setName(request.name());
            }

            if (request.description() != null) {
                region.setDescription(request.description());
            }

            if (request.technicalStaffIds() != null) {

                List<UUID> newIds = request.technicalStaffIds().stream()
                        .distinct()
                        .toList();

                List<UUID> currentIds = regionStaffRepository.findStaffIdsByRegionId(id);

                Set<UUID> currentSet = new HashSet<>(currentIds);
                Set<UUID> newSet = new HashSet<>(newIds);

                List<RegionStaff> toRemove = regionStaffRepository.findByIdRegionId(id)
                        .stream()
                        .filter(rs -> !newSet.contains(rs.getId().getStaffId()))
                        .toList();

                regionStaffRepository.deleteAll(toRemove);

                List<RegionStaff> toAdd = newSet.stream()
                        .filter(staffId -> !currentSet.contains(staffId))
                        .map(staffId -> RegionStaff.builder()
                                .id(new RegionStaffId(id, staffId))
                                .region(region)
                                .build())
                        .toList();

                regionStaffRepository.saveAll(toAdd);
            }

            regionRepository.save(region);

            List<UUID> finalStaffIds = regionStaffRepository.findStaffIdsByRegionId(id);

            return regionMapper.toDto(region, finalStaffIds);

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

        if (regionStaffRepository.existsById(id)) {
            throw new RuntimeException("Staff already in region");
        }

        RegionStaff rs = RegionStaff.builder()
                .id(id)
                .region(region)
                .build();
        regionStaffRepository.save(rs);
        List<UUID> staffIds = regionStaffRepository.findStaffIdsByRegionId(regionId);

        return regionMapper.toDto(region, staffIds);
    }

    @Override
    @Transactional
    public RegionDto removeStaffFromRegion(UUID regionId, UUID staffId) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new RuntimeException("Region not found"));

        RegionStaffId id = new RegionStaffId(regionId, staffId);

        if (!regionStaffRepository.existsById(id)) {
            throw new RuntimeException("Staff not found in region");
        }

        regionStaffRepository.deleteById(id);

        List<UUID> staffIds = regionStaffRepository.findStaffIdsByRegionId(regionId);

        return regionMapper.toDto(region, staffIds);
    }

    @Override
    @Transactional
    public List<RegionDto> getRegionByStaffId(UUID staffId) {
        try {
            List<RegionStaff> rsList = regionStaffRepository.findAllByIdStaffId(staffId);

            if (rsList.isEmpty()) {
                throw new RuntimeException("Staff not assigned to any region");
            }

            return rsList.stream()
                    .map(rs -> {
                        Region region = rs.getRegion();

                        List<UUID> staffIds = regionStaffRepository.findByIdRegionId(region.getId())
                                .stream()
                                .map(s -> s.getId().getStaffId())
                                .toList();

                        return regionMapper.toDto(region, staffIds);
                    })
                    .toList();

        } catch (Exception ex) {
            throw new RuntimeException("Cannot get region by staff: " + ex.getMessage());
        }
    }
}
