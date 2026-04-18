package com.isums.houseservice.services;

import com.isums.houseservice.domains.dtos.RegionDto.CreateRegionRequest;
import com.isums.houseservice.domains.dtos.RegionDto.RegionDto;
import com.isums.houseservice.domains.dtos.RegionDto.UpdateRegionRequest;
import com.isums.houseservice.domains.entities.Region;
import com.isums.houseservice.domains.entities.RegionStaff;
import com.isums.houseservice.domains.entities.RegionStaffId;
import com.isums.houseservice.exceptions.ConflictException;
import com.isums.houseservice.exceptions.NotFoundException;
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

@Service
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionService {
    private final RegionRepository regionRepository;
    private final RegionMapper regionMapper;
    private final RegionStaffRepository regionStaffRepository;
    private final TranslationAutoFillService translationAutoFillService;

    @Override
    @Transactional
    public RegionDto createRegion(String managerId, CreateRegionRequest request) {
        UUID managerUuid;
        try {
            managerUuid = UUID.fromString(managerId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid managerId: " + managerId);
        }

        Region region = Region.builder()
                .name(request.name())
                .nameTranslations(translationAutoFillService.complete(request.name()))
                .description(request.description())
                .descriptionTranslations(translationAutoFillService.complete(request.description()))
                .managerId(managerUuid)
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
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegionDto> getAllRegions() {
        List<Region> regions = regionRepository.findAll();

        return regions.stream().map(region -> {
            List<UUID> staffIds = regionStaffRepository.findStaffIdsByRegionId(region.getId());
            return regionMapper.toDto(region, staffIds);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RegionDto getById(UUID id) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Region not found"));
        List<UUID> staffIds = regionStaffRepository.findStaffIdsByRegionId(id);

        return regionMapper.toDto(region, staffIds);
    }

    @Override
    @Transactional
    public RegionDto updateRegion(UUID id, UpdateRegionRequest request) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Region not found"));

        if (request.name() != null) {
            region.setName(request.name());
            region.setNameTranslations(translationAutoFillService.complete(request.name()));
        }

        if (request.description() != null) {
            region.setDescription(request.description());
            region.setDescriptionTranslations(translationAutoFillService.complete(request.description()));
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
    }

    @Override
    @Transactional
    public RegionDto addStaffToRegion(UUID regionId, UUID staffId) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new NotFoundException("Region not found"));

        RegionStaffId id = new RegionStaffId(regionId, staffId);

        if (regionStaffRepository.existsById(id)) {
            throw new ConflictException("Staff already in region");
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
                .orElseThrow(() -> new NotFoundException("Region not found"));

        RegionStaffId id = new RegionStaffId(regionId, staffId);

        if (!regionStaffRepository.existsById(id)) {
            throw new NotFoundException("Staff not found in region");
        }

        regionStaffRepository.deleteById(id);

        List<UUID> staffIds = regionStaffRepository.findStaffIdsByRegionId(regionId);

        return regionMapper.toDto(region, staffIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegionDto> getRegionByStaffId(UUID staffId) {
        List<RegionStaff> rsList = regionStaffRepository.findAllByIdStaffId(staffId);

        if (rsList.isEmpty()) {
            throw new NotFoundException("Staff not assigned to any region");
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
    }
}
