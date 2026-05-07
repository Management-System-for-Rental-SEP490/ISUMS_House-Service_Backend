package com.isums.houseservice.services;

import com.isums.houseservice.domains.dtos.RegionDto.CreateRegionRequest;
import com.isums.houseservice.domains.dtos.RegionDto.RegionDto;
import com.isums.houseservice.domains.dtos.RegionDto.UpdateRegionRequest;
import com.isums.houseservice.domains.entities.Region;
import com.isums.houseservice.domains.entities.RegionStaff;
import com.isums.houseservice.domains.entities.RegionStaffId;
import com.isums.houseservice.exceptions.ConflictException;
import com.isums.houseservice.exceptions.NotFoundException;
import com.isums.houseservice.infrastructures.mappers.RegionMapper;
import com.isums.houseservice.infrastructures.repositories.RegionRepository;
import com.isums.houseservice.infrastructures.repositories.RegionStaffRepository;
import common.i18n.TranslationMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegionServiceImpl")
class RegionServiceImplTest {

    @Mock private RegionRepository regionRepository;
    @Mock private RegionMapper regionMapper;
    @Mock private RegionStaffRepository regionStaffRepository;
    @Mock private TranslationAutoFillService translationAutoFillService;

    @InjectMocks private RegionServiceImpl service;

    private UUID regionId;
    private UUID managerId;
    private UUID staffId1;
    private UUID staffId2;
    private Region region;

    @BeforeEach
    void setUp() {
        regionId = UUID.randomUUID();
        managerId = UUID.randomUUID();
        staffId1 = UUID.randomUUID();
        staffId2 = UUID.randomUUID();
        region = Region.builder()
                .id(regionId).name("North").description("North region")
                .managerId(managerId).build();
    }

    private RegionDto dto(List<UUID> staffs) {
        return new RegionDto(regionId, "North", "North region", managerId, staffs);
    }

    @Nested
    @DisplayName("createRegion")
    class CreateRegion {

        @Test
        @DisplayName("saves region and deduplicated staff links")
        void happyPathWithStaff() {
            CreateRegionRequest req = new CreateRegionRequest(
                    "North", "North region", managerId, List.of(staffId1, staffId2, staffId1));
            RegionDto expected = dto(List.of(staffId1, staffId2));
            when(translationAutoFillService.complete("North"))
                    .thenReturn(TranslationMap.of(java.util.Map.of("vi", "North", "en", "North", "ja", "ノース")));
            when(translationAutoFillService.complete("North region"))
                    .thenReturn(TranslationMap.of(java.util.Map.of("vi", "North region", "en", "North region", "ja", "北リージョン")));

            when(regionMapper.toDto(any(Region.class), anyList())).thenReturn(expected);

            RegionDto res = service.createRegion(managerId.toString(), req);

            assertThat(res).isSameAs(expected);
            verify(regionRepository).save(any(Region.class));

            ArgumentCaptor<List<RegionStaff>> cap = ArgumentCaptor.forClass(List.class);
            verify(regionStaffRepository).saveAll(cap.capture());
            assertThat(cap.getValue()).hasSize(2);
        }

        @Test
        @DisplayName("skips staff save when technicalStaffIds is null")
        void nullStaffIds() {
            CreateRegionRequest req = new CreateRegionRequest("North", "desc", managerId, null);
            when(translationAutoFillService.complete("North"))
                    .thenReturn(TranslationMap.of(java.util.Map.of("vi", "North", "en", "North", "ja", "ノース")));
            when(translationAutoFillService.complete("desc"))
                    .thenReturn(TranslationMap.of(java.util.Map.of("vi", "desc", "en", "desc", "ja", "説明")));
            when(regionMapper.toDto(any(Region.class), anyList())).thenReturn(dto(List.of()));

            service.createRegion(managerId.toString(), req);

            verify(regionStaffRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("skips staff save when technicalStaffIds is empty")
        void emptyStaffIds() {
            CreateRegionRequest req = new CreateRegionRequest("N", "d", managerId, List.of());
            when(translationAutoFillService.complete("N"))
                    .thenReturn(TranslationMap.of(java.util.Map.of("vi", "N", "en", "N", "ja", "N")));
            when(translationAutoFillService.complete("d"))
                    .thenReturn(TranslationMap.of(java.util.Map.of("vi", "d", "en", "d", "ja", "d")));
            when(regionMapper.toDto(any(Region.class), anyList())).thenReturn(dto(List.of()));

            service.createRegion(managerId.toString(), req);

            verify(regionStaffRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("throws IllegalArgumentException for invalid managerId")
        void invalidManagerId() {
            CreateRegionRequest req = new CreateRegionRequest("N", "d", null, null);

            assertThatThrownBy(() -> service.createRegion("not-a-uuid", req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid managerId");
            verify(regionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAllRegions")
    class GetAllRegions {

        @Test
        @DisplayName("returns mapped list with per-region staffIds")
        void returnsList() {
            when(regionRepository.findAll()).thenReturn(List.of(region));
            when(regionStaffRepository.findStaffIdsByRegionId(regionId))
                    .thenReturn(List.of(staffId1));
            RegionDto expected = dto(List.of(staffId1));
            when(regionMapper.toDto(region, List.of(staffId1))).thenReturn(expected);

            assertThat(service.getAllRegions()).containsExactly(expected);
        }

        @Test
        @DisplayName("returns empty list when no regions")
        void empty() {
            when(regionRepository.findAll()).thenReturn(Collections.emptyList());
            assertThat(service.getAllRegions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("returns dto when region exists")
        void existing() {
            when(regionRepository.findById(regionId)).thenReturn(Optional.of(region));
            when(regionStaffRepository.findStaffIdsByRegionId(regionId)).thenReturn(List.of(staffId1));
            RegionDto expected = dto(List.of(staffId1));
            when(regionMapper.toDto(region, List.of(staffId1))).thenReturn(expected);

            assertThat(service.getById(regionId)).isSameAs(expected);
        }

        @Test
        @DisplayName("throws NotFoundException when region missing")
        void notFound() {
            when(regionRepository.findById(regionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(regionId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Region not found");
        }
    }

    @Nested
    @DisplayName("updateRegion")
    class UpdateRegion {

        @Test
        @DisplayName("updates name only when description is null")
        void partialUpdateFields() {
            UpdateRegionRequest req = new UpdateRegionRequest("Updated", null, null, null);
            when(regionRepository.findById(regionId)).thenReturn(Optional.of(region));
            when(translationAutoFillService.complete("Updated"))
                    .thenReturn(TranslationMap.of(java.util.Map.of("vi", "Updated", "en", "Updated", "ja", "更新")));
            when(regionStaffRepository.findStaffIdsByRegionId(regionId)).thenReturn(List.of());
            when(regionMapper.toDto(any(Region.class), anyList())).thenReturn(dto(List.of()));

            service.updateRegion(regionId, req);

            assertThat(region.getName()).isEqualTo("Updated");
            assertThat(region.getDescription()).isEqualTo("North region");
            verify(regionRepository).save(region);
            verify(regionStaffRepository, never()).deleteAll(any());
            verify(regionStaffRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("reconciles staff list: removes obsolete and adds new distinct")
        void reconcilesStaff() {
            UUID newStaff = UUID.randomUUID();
            UpdateRegionRequest req = new UpdateRegionRequest(
                    null, null, null, List.of(staffId1, newStaff, staffId1));

            when(regionRepository.findById(regionId)).thenReturn(Optional.of(region));
            when(regionStaffRepository.findStaffIdsByRegionId(regionId))
                    .thenReturn(List.of(staffId1, staffId2));
            RegionStaff existing1 = RegionStaff.builder().id(new RegionStaffId(regionId, staffId1)).region(region).build();
            RegionStaff existing2 = RegionStaff.builder().id(new RegionStaffId(regionId, staffId2)).region(region).build();
            when(regionStaffRepository.findByIdRegionId(regionId)).thenReturn(List.of(existing1, existing2));
            when(regionMapper.toDto(any(Region.class), anyList())).thenReturn(dto(List.of()));

            service.updateRegion(regionId, req);

            ArgumentCaptor<List<RegionStaff>> removeCap = ArgumentCaptor.forClass(List.class);
            verify(regionStaffRepository).deleteAll(removeCap.capture());
            assertThat(removeCap.getValue()).containsExactly(existing2);

            ArgumentCaptor<List<RegionStaff>> addCap = ArgumentCaptor.forClass(List.class);
            verify(regionStaffRepository).saveAll(addCap.capture());
            assertThat(addCap.getValue()).hasSize(1);
            assertThat(addCap.getValue().get(0).getId().getStaffId()).isEqualTo(newStaff);
        }

        @Test
        @DisplayName("throws NotFoundException when region missing")
        void notFound() {
            when(regionRepository.findById(regionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateRegion(regionId,
                    new UpdateRegionRequest("x", null, null, null)))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Region not found");
        }
    }

    @Nested
    @DisplayName("addStaffToRegion")
    class AddStaffToRegion {

        @Test
        @DisplayName("saves new link and returns dto")
        void addsNew() {
            when(regionRepository.findById(regionId)).thenReturn(Optional.of(region));
            when(regionStaffRepository.existsById(any(RegionStaffId.class))).thenReturn(false);
            when(regionStaffRepository.findStaffIdsByRegionId(regionId)).thenReturn(List.of(staffId1));
            RegionDto expected = dto(List.of(staffId1));
            when(regionMapper.toDto(region, List.of(staffId1))).thenReturn(expected);

            assertThat(service.addStaffToRegion(regionId, staffId1)).isSameAs(expected);

            ArgumentCaptor<RegionStaff> cap = ArgumentCaptor.forClass(RegionStaff.class);
            verify(regionStaffRepository).save(cap.capture());
            assertThat(cap.getValue().getId().getStaffId()).isEqualTo(staffId1);
        }

        @Test
        @DisplayName("throws NotFoundException when region missing")
        void regionMissing() {
            when(regionRepository.findById(regionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addStaffToRegion(regionId, staffId1))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Region not found");
        }

        @Test
        @DisplayName("throws ConflictException on duplicate")
        void duplicateStaff() {
            when(regionRepository.findById(regionId)).thenReturn(Optional.of(region));
            when(regionStaffRepository.existsById(any(RegionStaffId.class))).thenReturn(true);

            assertThatThrownBy(() -> service.addStaffToRegion(regionId, staffId1))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Staff already in region");
            verify(regionStaffRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("removeStaffFromRegion")
    class RemoveStaffFromRegion {

        @Test
        @DisplayName("deletes link and returns updated dto")
        void removes() {
            when(regionRepository.findById(regionId)).thenReturn(Optional.of(region));
            when(regionStaffRepository.existsById(any(RegionStaffId.class))).thenReturn(true);
            when(regionStaffRepository.findStaffIdsByRegionId(regionId)).thenReturn(List.of());
            RegionDto expected = dto(List.of());
            when(regionMapper.toDto(region, List.of())).thenReturn(expected);

            assertThat(service.removeStaffFromRegion(regionId, staffId1)).isSameAs(expected);

            verify(regionStaffRepository).deleteById(any(RegionStaffId.class));
        }

        @Test
        @DisplayName("throws NotFoundException when region missing")
        void regionMissing() {
            when(regionRepository.findById(regionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeStaffFromRegion(regionId, staffId1))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Region not found");
        }

        @Test
        @DisplayName("throws NotFoundException when staff not linked")
        void staffNotLinked() {
            when(regionRepository.findById(regionId)).thenReturn(Optional.of(region));
            when(regionStaffRepository.existsById(any(RegionStaffId.class))).thenReturn(false);

            assertThatThrownBy(() -> service.removeStaffFromRegion(regionId, staffId1))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Staff not found in region");
            verify(regionStaffRepository, never()).deleteById(any(RegionStaffId.class));
        }
    }

    @Nested
    @DisplayName("getRegionByStaffId")
    class GetRegionByStaffId {

        @Test
        @DisplayName("returns dto list for each region the staff belongs to")
        void returns() {
            RegionStaff rs = RegionStaff.builder().id(new RegionStaffId(regionId, staffId1)).region(region).build();
            when(regionStaffRepository.findAllByIdStaffId(staffId1)).thenReturn(List.of(rs));
            when(regionStaffRepository.findByIdRegionId(regionId)).thenReturn(List.of(rs));
            RegionDto expected = dto(List.of(staffId1));
            when(regionMapper.toDto(region, List.of(staffId1))).thenReturn(expected);

            assertThat(service.getRegionByStaffId(staffId1)).containsExactly(expected);
        }

        @Test
        @DisplayName("throws NotFoundException when no assignments")
        void empty() {
            when(regionStaffRepository.findAllByIdStaffId(staffId1))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.getRegionByStaffId(staffId1))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Staff not assigned to any region");
        }
    }
}
