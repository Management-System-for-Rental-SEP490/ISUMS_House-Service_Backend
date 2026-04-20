package com.isums.houseservice.services;

import com.isums.houseservice.domains.dtos.FunctionalAreaDto.CreateFunctionalAreaRequest;
import com.isums.houseservice.domains.dtos.FunctionalAreaDto.FunctionalAreaDto;
import com.isums.houseservice.domains.dtos.FunctionalAreaDto.UpdateFunctionalAreaRequest;
import com.isums.houseservice.domains.emuns.AreaType;
import com.isums.houseservice.domains.emuns.FuctionalAreaStatus;
import com.isums.houseservice.domains.entities.FunctionalArea;
import com.isums.houseservice.domains.entities.House;
import com.isums.houseservice.domains.mapper.FunctionalAreaMapper;
import com.isums.houseservice.exceptions.NotFoundException;
import com.isums.houseservice.infrastructures.repositories.FunctionalAreaRepository;
import com.isums.houseservice.infrastructures.repositories.HouseRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FunctionAreaServiceImpl")
class FunctionAreaServiceImplTest {

    @Mock private HouseRepository houseRepository;
    @Mock private FunctionalAreaMapper functionalAreaMapper;
    @Mock private FunctionalAreaRepository functionalAreaRepository;
    @Mock private TranslationAutoFillService translationAutoFillService;

    @InjectMocks private FunctionAreaServiceImpl service;

    private UUID houseId;
    private UUID areaId;
    private House house;

    @BeforeEach
    void setUp() {
        houseId = UUID.randomUUID();
        areaId = UUID.randomUUID();
        house = House.builder().id(houseId).name("H1").build();
    }

    private FunctionalAreaDto dto() {
        return new FunctionalAreaDto(areaId, houseId, "Kitchen", AreaType.KITCHEN,
                "1", "desc", FuctionalAreaStatus.NORMAL, Instant.now(), Instant.now(), null);
    }

    @Nested
    @DisplayName("createArea")
    class CreateArea {

        @Test
        @DisplayName("creates area with NORMAL status when house exists")
        void happyPath() {
            CreateFunctionalAreaRequest req = new CreateFunctionalAreaRequest(
                    houseId, "Kitchen", AreaType.KITCHEN, "1", "desc");
            when(translationAutoFillService.complete("Kitchen"))
                    .thenReturn(TranslationMap.of(java.util.Map.of("vi", "Kitchen", "en", "Kitchen", "ja", "キッチン")));
            when(translationAutoFillService.complete("desc"))
                    .thenReturn(TranslationMap.of(java.util.Map.of("vi", "desc", "en", "desc", "ja", "説明")));

            when(houseRepository.findById(houseId)).thenReturn(Optional.of(house));
            when(functionalAreaRepository.save(any(FunctionalArea.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            FunctionalAreaDto expected = dto();
            when(functionalAreaMapper.mapFunc(any(FunctionalArea.class))).thenReturn(expected);

            assertThat(service.createArea(req)).isSameAs(expected);

            ArgumentCaptor<FunctionalArea> cap = ArgumentCaptor.forClass(FunctionalArea.class);
            verify(functionalAreaRepository).save(cap.capture());
            FunctionalArea saved = cap.getValue();
            assertThat(saved.getHouse()).isSameAs(house);
            assertThat(saved.getName()).isEqualTo("Kitchen");
            assertThat(saved.getAreaType()).isEqualTo(AreaType.KITCHEN);
            assertThat(saved.getStatus()).isEqualTo(FuctionalAreaStatus.NORMAL);
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("throws NotFoundException when house missing")
        void houseMissing() {
            CreateFunctionalAreaRequest req = new CreateFunctionalAreaRequest(
                    houseId, "Kitchen", AreaType.KITCHEN, "1", "desc");

            when(houseRepository.findById(houseId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createArea(req))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("House not found");
            verify(functionalAreaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAllAreas")
    class GetAllAreas {

        @Test
        @DisplayName("returns mapped list for the house")
        void returnsList() {
            FunctionalArea area = FunctionalArea.builder().id(areaId).house(house).build();
            when(functionalAreaRepository.findByHouseId(houseId)).thenReturn(List.of(area));
            when(functionalAreaMapper.mapFuncs(List.of(area))).thenReturn(List.of(dto()));

            assertThat(service.getAllAreas(houseId)).hasSize(1);
        }

        @Test
        @DisplayName("returns empty list when no areas")
        void empty() {
            when(functionalAreaRepository.findByHouseId(houseId)).thenReturn(List.of());
            when(functionalAreaMapper.mapFuncs(List.of())).thenReturn(List.of());

            assertThat(service.getAllAreas(houseId)).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateArea")
    class UpdateArea {

        private UpdateFunctionalAreaRequest fullReq() {
            return new UpdateFunctionalAreaRequest(
                    "NewName", AreaType.BEDROOM, "2", "newDesc",
                    FuctionalAreaStatus.REPAIRED, Instant.now());
        }

        @Test
        @DisplayName("updates all fields when all present")
        void updatesAll() {
            FunctionalArea area = FunctionalArea.builder()
                    .id(areaId).house(house).name("old")
                    .areaType(AreaType.KITCHEN).floorNo("1")
                    .description("old").status(FuctionalAreaStatus.NORMAL).build();

            when(functionalAreaRepository.findById(areaId)).thenReturn(Optional.of(area));
            when(translationAutoFillService.complete("NewName"))
                    .thenReturn(TranslationMap.of(java.util.Map.of("vi", "NewName", "en", "NewName", "ja", "ニュー")));
            when(translationAutoFillService.complete("newDesc"))
                    .thenReturn(TranslationMap.of(java.util.Map.of("vi", "newDesc", "en", "newDesc", "ja", "新説明")));
            when(functionalAreaRepository.save(area)).thenReturn(area);
            when(functionalAreaMapper.mapFunc(area)).thenReturn(dto());

            service.updateArea(areaId, fullReq());

            assertThat(area.getName()).isEqualTo("NewName");
            assertThat(area.getAreaType()).isEqualTo(AreaType.BEDROOM);
            assertThat(area.getFloorNo()).isEqualTo("2");
            assertThat(area.getDescription()).isEqualTo("newDesc");
            assertThat(area.getStatus()).isEqualTo(FuctionalAreaStatus.REPAIRED);
        }

        @Test
        @DisplayName("only updates fields that are non-null")
        void partialUpdate() {
            FunctionalArea area = FunctionalArea.builder()
                    .id(areaId).name("old").areaType(AreaType.KITCHEN)
                    .floorNo("1").description("old").status(FuctionalAreaStatus.NORMAL).build();

            when(functionalAreaRepository.findById(areaId)).thenReturn(Optional.of(area));
            when(translationAutoFillService.complete("NewName"))
                    .thenReturn(TranslationMap.of(java.util.Map.of("vi", "NewName", "en", "NewName", "ja", "ニュー")));
            when(functionalAreaRepository.save(area)).thenReturn(area);
            when(functionalAreaMapper.mapFunc(area)).thenReturn(dto());

            UpdateFunctionalAreaRequest req = new UpdateFunctionalAreaRequest(
                    "NewName", null, null, null, null, null);

            service.updateArea(areaId, req);

            assertThat(area.getName()).isEqualTo("NewName");
            assertThat(area.getAreaType()).isEqualTo(AreaType.KITCHEN);
            assertThat(area.getDescription()).isEqualTo("old");
        }

        @Test
        @DisplayName("throws NotFoundException when area missing")
        void notFound() {
            when(functionalAreaRepository.findById(areaId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateArea(areaId, fullReq()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Functional area not found");
        }
    }

    @Nested
    @DisplayName("deleteArea")
    class DeleteArea {

        @Test
        @DisplayName("soft deletes by setting status UNAVAILABLE and returns true")
        void softDeletes() {
            FunctionalArea area = FunctionalArea.builder()
                    .id(areaId).status(FuctionalAreaStatus.NORMAL).build();

            when(functionalAreaRepository.findById(areaId)).thenReturn(Optional.of(area));

            assertThat(service.deleteArea(areaId)).isTrue();
            assertThat(area.getStatus()).isEqualTo(FuctionalAreaStatus.UNAVAILABLE);
            verify(functionalAreaRepository).save(area);
        }

        @Test
        @DisplayName("throws NotFoundException when area missing")
        void notFound() {
            when(functionalAreaRepository.findById(areaId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteArea(areaId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Functional area not found");
            verify(functionalAreaRepository, never()).save(any());
        }
    }
}
