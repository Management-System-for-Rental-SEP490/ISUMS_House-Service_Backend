package com.isums.houseservice.services;

import com.isums.houseservice.domains.dtos.CreateHouseRequest;
import com.isums.houseservice.domains.dtos.FunctionalAreaDto.FunctionalAreaDto;
import com.isums.houseservice.domains.dtos.HouseAccessStatus;
import com.isums.houseservice.domains.dtos.HouseDto;
import com.isums.houseservice.domains.dtos.HouseHistoryItemDto;
import com.isums.houseservice.domains.dtos.InvoiceStatusDto;
import com.isums.houseservice.domains.dtos.UpdateHouseTranslationsRequest;
import com.isums.houseservice.domains.emuns.AccessStatus;
import com.isums.houseservice.domains.emuns.AreaType;
import com.isums.houseservice.domains.emuns.FuctionalAreaStatus;
import com.isums.houseservice.domains.emuns.HouseMemberRole;
import com.isums.houseservice.domains.emuns.HouseStatus;
import com.isums.houseservice.domains.entities.*;
import com.isums.houseservice.exceptions.NotFoundException;
import com.isums.houseservice.infrastructures.clients.AssetRestClient;
import com.isums.houseservice.infrastructures.clients.HouseHistoryRestClient;
import com.isums.houseservice.infrastructures.grpcs.PaymentGrpcClient;
import com.isums.houseservice.infrastructures.grpcs.UserClientsGrpc;
import com.isums.houseservice.infrastructures.kafkas.HouseEventProducer;
import com.isums.houseservice.infrastructures.kafkas.HouseSubscriptionNotifier;
import com.isums.houseservice.infrastructures.mappers.HouseMapper;
import com.isums.houseservice.infrastructures.repositories.*;
import com.isums.userservice.grpc.UserResponse;
import common.i18n.TranslationMap;
import common.paginations.cache.CachedPageService;
import common.paginations.dtos.PageRequest;
import common.paginations.dtos.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

@DisplayName("HouseServiceImpl Unit Tests")
class HouseServiceImplTest {

    // ─── Mocks ───────────────────────────────────────────────────────────────

    @Mock private HouseRepository          houseRepository;
    @Mock private HouseMapper              houseMapper;
    @Mock private RegionRepository         regionRepository;
    @Mock private HouseEventProducer       houseEventProducer;
    @Mock private S3ServiceImpl            s3;
    @Mock private HouseImageRepository     houseImageRepository;
    @Mock private UserClientsGrpc          userClientsGrpc;
    @Mock private AssetRestClient          assetRestClient;
    @Mock private HouseHistoryRestClient   houseHistoryRestClient;
    @Mock private PaymentGrpcClient        paymentGrpcClient;
    @Mock private TenantGroupRepository    tenantGroupRepository;
    @Mock private TenantMemberRepository   tenantMemberRepository;
    @Mock private CachedPageService        cachedPageService;
    @Mock private TranslationAutoFillService translationAutoFillService;
    @Mock private HouseSubscriptionNotifier subscriptionNotifier;

    @InjectMocks
    private HouseServiceImpl houseService;

    // ─── Common fixtures ─────────────────────────────────────────────────────

    private UUID   houseId;
    private UUID   userId;
    private UUID   regionId;
    private UUID   imageId;
    private Region region;
    private House  house;
    private HouseDto houseDto;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        // Default lenient stubs for both overloads of complete(...)
        lenient().when(translationAutoFillService.complete(anyString()))
                .thenAnswer(invocation -> {
                    String text = invocation.getArgument(0, String.class);
                    if (text == null) {
                        return null;
                    }
                    return TranslationMap.of(java.util.Map.of(
                            "vi", text,
                            "en", text,
                            "ja", text
                    ));
                });
        lenient().when(translationAutoFillService.complete(anyString(), any()))
                .thenAnswer(invocation -> {
                    String text = invocation.getArgument(0, String.class);
                    @SuppressWarnings("unchecked")
                    Map<String, String> overrides = invocation.getArgument(1, Map.class);
                    if (text == null || text.isBlank()) {
                        return null;
                    }
                    Map<String, String> out = new java.util.LinkedHashMap<>();
                    out.put("vi", text);
                    out.put("en", text);
                    out.put("ja", text);
                    if (overrides != null) {
                        for (Map.Entry<String, String> e : overrides.entrySet()) {
                            if (e.getKey() == null || e.getKey().isBlank()) continue;
                            if (e.getValue() == null || e.getValue().isBlank()) continue;
                            out.put(e.getKey().trim().toLowerCase(java.util.Locale.ROOT), e.getValue().trim());
                        }
                    }
                    return TranslationMap.of(out);
                });
        houseId  = UUID.randomUUID();
        userId   = UUID.randomUUID();
        regionId = UUID.randomUUID();
        imageId  = UUID.randomUUID();

        region = Region.builder()
                .id(regionId)
                .name("Test Region")
                .managerId(UUID.randomUUID())
                .build();

        house = House.builder()
                .id(houseId)
                .name("Test House")
                .address("123 Street")
                .ward("Ward 1")
                .commune("Test Commune")
                .city("Test City")
                .description("Desc")
                .paymentRestricted(false)
                .status(HouseStatus.AVAILABLE)
                .region(region)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        houseDto = new HouseDto(
                houseId, null, regionId,
                "Test House", "123 Street",
                "Ward 1", "Test Commune", "Test City",
                null,                              // numberOfFloors
                null, null, null, null, null,      // areaM2, structure, landCert_*
                false, "Desc",
                HouseStatus.AVAILABLE, List.of(), List.of(),
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of()
        );
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CreateHouse
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("CreateHouse")
    class CreateHouseTests {

        private CreateHouseRequest req;

        @BeforeEach
        void setUp() {
            req = new CreateHouseRequest(
                    "Test House", "123 Street", regionId,
                    "Ward 1", "Test Commune", "Test City", "Desc", 3,
                    null, null, null, null, null,   // areaM2, structure, landCert_*
                    List.of(),
                    null, null, null, null, null, null
            );
        }

        @Test
        @DisplayName("should create house, publish event, and return dto on success")
        void createHouse_success() {
            given(regionRepository.findById(regionId)).willReturn(Optional.of(region));
            given(houseRepository.save(any(House.class))).willReturn(house);
            given(houseMapper.toDto(house)).willReturn(houseDto);

            HouseDto result = houseService.CreateHouse(req);

            assertThat(result).isEqualTo(houseDto);
            verify(houseRepository).save(any(House.class));
            verify(cachedPageService).evictAll("houses");
            verify(houseEventProducer).publishHouseCreated(house.getId());
        }

        @Test
        @DisplayName("saved house has AVAILABLE status and correct fields (no manual overrides)")
        void createHouse_persistedEntityHasCorrectFields() {
            given(regionRepository.findById(regionId)).willReturn(Optional.of(region));
            given(houseRepository.save(any(House.class))).willReturn(house);
            given(houseMapper.toDto(house)).willReturn(houseDto);

            houseService.CreateHouse(req);

            ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
            verify(houseRepository).save(captor.capture());

            House saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(HouseStatus.AVAILABLE);
            assertThat(saved.getName()).isEqualTo("Test House");
            assertThat(saved.getWard()).isEqualTo("Ward 1");
            assertThat(saved.getNumberOfFloors()).isEqualTo(3);
            assertThat(saved.getPaymentRestricted()).isFalse();
            assertThat(saved.getFunctionalAreas()).isEmpty();
            assertThat(saved.getHouseImages()).isEmpty();
            assertThat(saved.getRegion()).isEqualTo(region);
            assertThat(saved.getCreatedAt()).isNotNull();

            // TranslationAutoFillService invoked with null overrides for every translatable field
            verify(translationAutoFillService).complete("Test House", null);
            verify(translationAutoFillService).complete("123 Street", null);
            verify(translationAutoFillService).complete("Ward 1", null);
            verify(translationAutoFillService).complete("Test Commune", null);
            verify(translationAutoFillService).complete("Test City", null);
            verify(translationAutoFillService).complete("Desc", null);
        }

        @Test
        @DisplayName("manager-supplied translation overrides are propagated to the translation service per field")
        void createHouse_withManualOverrides_propagatesToService() {
            CreateHouseRequest reqWithOverrides = new CreateHouseRequest(
                    "Nhà Hiệu Q7", "123 NVL", regionId,
                    "P.Tân Phong", "", "TP.HCM", "Nhà 3 tầng", 3,
                    null, null, null, null, null,                   // areaM2, structure, landCert_*
                    List.of(),
                    Map.of("en", "Hieu Mansion D7"),              // override EN only → AI fills JA
                    Map.of("en", "123 NVL addr", "ja", "123 NVL 住所"), // override EN + JA
                    null,                                          // all AI
                    null,
                    Map.of("ja", "HCM市（手動）"),                   // override JA only
                    null
            );

            given(regionRepository.findById(regionId)).willReturn(Optional.of(region));
            given(houseRepository.save(any(House.class))).willReturn(house);
            given(houseMapper.toDto(house)).willReturn(houseDto);

            houseService.CreateHouse(reqWithOverrides);

            verify(translationAutoFillService).complete(eq("Nhà Hiệu Q7"),
                    eq(Map.of("en", "Hieu Mansion D7")));
            verify(translationAutoFillService).complete(eq("123 NVL"),
                    eq(Map.of("en", "123 NVL addr", "ja", "123 NVL 住所")));
            verify(translationAutoFillService).complete("P.Tân Phong", null);
            verify(translationAutoFillService).complete("", null);
            verify(translationAutoFillService).complete(eq("TP.HCM"),
                    eq(Map.of("ja", "HCM市（手動）")));
            verify(translationAutoFillService).complete("Nhà 3 tầng", null);
        }

        @Test
        @DisplayName("saved TranslationMap contains manager overrides alongside auto-filled locales")
        void createHouse_persistedTranslationsContainOverrides() {
            CreateHouseRequest reqWithOverrides = new CreateHouseRequest(
                    "Nhà Hiệu Q7", "123 Street", regionId,
                    "Ward 1", "Test Commune", "Test City", "Desc", 3,
                    null, null, null, null, null,                   // areaM2, structure, landCert_*
                    List.of(),
                    Map.of("en", "Hieu Mansion D7"),
                    null, null, null, null, null
            );

            given(regionRepository.findById(regionId)).willReturn(Optional.of(region));
            given(houseRepository.save(any(House.class))).willReturn(house);
            given(houseMapper.toDto(house)).willReturn(houseDto);

            houseService.CreateHouse(reqWithOverrides);

            ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
            verify(houseRepository).save(captor.capture());
            House saved = captor.getValue();

            Map<String, String> nameMap = saved.getNameTranslations().getTranslations();
            // Our lenient stub echoes text for ai locales and honours overrides → expect en=override, ja=auto
            assertThat(nameMap).containsEntry("en", "Hieu Mansion D7");
            assertThat(nameMap).containsEntry("vi", "Nhà Hiệu Q7");
            assertThat(nameMap).containsEntry("ja", "Nhà Hiệu Q7"); // stub's auto-fill echoes source
        }

        @Test
        @DisplayName("should throw RuntimeException when region not found")
        void createHouse_regionNotFound_throwsException() {
            given(regionRepository.findById(regionId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> houseService.CreateHouse(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Region not found");

            verifyNoInteractions(houseRepository, houseEventProducer);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getHouseById
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getHouseById")
    class GetHouseByIdTests {

        @Test
        @DisplayName("should return dto when house exists")
        void getHouseById_success() {
            given(houseRepository.findWithFunctionalAreasById(houseId)).willReturn(Optional.of(house));
            given(houseMapper.toDto(house)).willReturn(houseDto);

            HouseDto result = houseService.getHouseById(houseId);

            assertThat(result).isEqualTo(houseDto);
        }

        @Test
        @DisplayName("should enrich functionalAreas with assetCount from asset-service")
        void getHouseById_enrichAssetCount() {
            UUID kitchenId = UUID.randomUUID();
            UUID livingRoomId = UUID.randomUUID();

            HouseDto dtoWithAreas = new HouseDto(
                    houseId, null, regionId,
                    "Test House", "123 Street",
                    "Ward 1", "Test Commune", "Test City",
                    null,                              // numberOfFloors
                    null, null, null, null, null,      // areaM2, structure, landCert_*
                    false, "Desc",
                    HouseStatus.AVAILABLE,
                    List.of(
                            new FunctionalAreaDto(kitchenId, houseId, "Kitchen", AreaType.KITCHEN, "1", "desc", FuctionalAreaStatus.NORMAL, Instant.now(), Instant.now(), null),
                            new FunctionalAreaDto(livingRoomId, houseId, "Living", AreaType.LIVINGROOM, "1", "desc", FuctionalAreaStatus.NORMAL, Instant.now(), Instant.now(), null)
                    ),
                    List.of(),
                    Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of()
            );

            Jwt jwt = Jwt.withTokenValue("test-token")
                    .header("alg", "none")
                    .claim("sub", "tester")
                    .build();
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null));

            given(houseRepository.findWithFunctionalAreasById(houseId)).willReturn(Optional.of(house));
            given(houseMapper.toDto(house)).willReturn(dtoWithAreas);
            given(assetRestClient.getAssetCountByHouseId(houseId, "test-token")).willReturn(List.of(
                    new AssetRestClient.AreaAssetCountDto(kitchenId, 2),
                    new AssetRestClient.AreaAssetCountDto(livingRoomId, 1),
                    new AssetRestClient.AreaAssetCountDto(null, 99)
            ));

            HouseDto result = houseService.getHouseById(houseId);

            assertThat(result.functionalAreas()).hasSize(2);
            assertThat(result.functionalAreas().get(0).assetCount()).isEqualTo(2);
            assertThat(result.functionalAreas().get(1).assetCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should throw RuntimeException when house not found")
        void getHouseById_notFound_throwsException() {
            given(houseRepository.findWithFunctionalAreasById(houseId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> houseService.getHouseById(houseId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("House not found");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getHouseHistory")
    class GetHouseHistoryTests {

        @Test
        @DisplayName("should aggregate maintenance, inspection, and issue items sorted by happenedAt desc")
        void getHouseHistory_success() {
            Jwt jwt = Jwt.withTokenValue("test-token")
                    .header("alg", "none")
                    .claim("sub", "tester")
                    .build();
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null));

            given(houseRepository.existsById(houseId)).willReturn(true);
            given(houseHistoryRestClient.getMaintenanceJobsByHouseId(houseId, "test-token"))
                    .willReturn(List.of(
                            new HouseHistoryRestClient.MaintenanceJobItemDto(
                                    UUID.randomUUID(), UUID.randomUUID(), houseId, UUID.randomUUID(),
                                    "Staff A", "0901", null, LocalDate.of(2026, 4, 16), "CREATED"
                            )
                    ));
            given(houseHistoryRestClient.getInspectionsByHouseId(houseId, "test-token"))
                    .willReturn(List.of(
                            new HouseHistoryRestClient.InspectionItemDto(
                                    UUID.randomUUID(), houseId, UUID.randomUUID(), UUID.randomUUID(),
                                    "Staff B", "0902", UUID.randomUUID(), "DONE", "CHECK_IN", "handover",
                                    Instant.parse("2026-04-17T08:00:00Z"),
                                    Instant.parse("2026-04-17T09:00:00Z")
                            )
                    ));
            given(houseHistoryRestClient.getIssueTicketsByHouseId(houseId, "test-token"))
                    .willReturn(List.of(
                            new HouseHistoryRestClient.IssueTicketItemDto(
                                    UUID.randomUUID(), UUID.randomUUID(), "0903", houseId, UUID.randomUUID(), UUID.randomUUID(),
                                    "Staff C", "0904", UUID.randomUUID(),
                                    LocalDateTime.of(2026, 4, 18, 10, 30), LocalDateTime.of(2026, 4, 18, 11, 0),
                                    "REPAIR", "IN_PROGRESS", "Sửa máy lạnh", "máy lạnh chảy nước",
                                    Instant.parse("2026-04-18T03:00:00Z"),
                                    List.of(), null, null, null, null
                            )
                    ));

            List<HouseHistoryItemDto> result = houseService.getHouseHistory(houseId);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).source()).isEqualTo("ISSUE");
            assertThat(result.get(0).title()).isEqualTo("Sửa máy lạnh");
            assertThat(result.get(1).source()).isEqualTo("INSPECTION");
            assertThat(result.get(1).type()).isEqualTo("CHECK_IN");
            assertThat(result.get(2).source()).isEqualTo("MAINTENANCE");
            assertThat(result.get(2).type()).isEqualTo("PERIODIC_MAINTENANCE");
        }

        @Test
        @DisplayName("should return partial history when one downstream source fails")
        void getHouseHistory_partialWhenOneSourceFails() {
            Jwt jwt = Jwt.withTokenValue("test-token")
                    .header("alg", "none")
                    .claim("sub", "tester")
                    .build();
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null));

            given(houseRepository.existsById(houseId)).willReturn(true);
            given(houseHistoryRestClient.getMaintenanceJobsByHouseId(houseId, "test-token"))
                    .willThrow(new RuntimeException("maintenance down"));
            given(houseHistoryRestClient.getInspectionsByHouseId(houseId, "test-token"))
                    .willReturn(List.of());
            given(houseHistoryRestClient.getIssueTicketsByHouseId(houseId, "test-token"))
                    .willReturn(List.of(
                            new HouseHistoryRestClient.IssueTicketItemDto(
                                    UUID.randomUUID(), UUID.randomUUID(), "0903", houseId, null, null,
                                    null, null, null,
                                    null, null,
                                    "REPAIR", "CREATED", "Sửa điện", "mất điện",
                                    Instant.parse("2026-04-18T03:00:00Z"),
                                    List.of(), null, null, null, null
                            )
                    ));

            List<HouseHistoryItemDto> result = houseService.getHouseHistory(houseId);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().source()).isEqualTo("ISSUE");
        }
    }

    // getHouseByUserId
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getHouseByUserId")
    class GetHouseByUserIdTests {

        private final String keycloakId = "keycloak-abc";

        @Test
        @DisplayName("should return list of dtos for valid user")
        void getHouseByUserId_success() {
            UserResponse userResponse = UserResponse.newBuilder()
                    .setId(userId.toString())
                    .build();
            given(userClientsGrpc.getUserIdAndRoleByKeyCloakId(keycloakId)).willReturn(userResponse);
            given(houseRepository.findByUserRentalId(userId)).willReturn(List.of(house));
            given(houseMapper.toDtos(List.of(house))).willReturn(List.of(houseDto));

            List<HouseDto> result = houseService.getHouseByUserId(keycloakId);

            assertThat(result).containsExactly(houseDto);
        }

        @Test
        @DisplayName("should throw RuntimeException when gRPC call fails")
        void getHouseByUserId_grpcFails_throwsRuntimeException() {
            given(userClientsGrpc.getUserIdAndRoleByKeyCloakId(keycloakId))
                    .willThrow(new RuntimeException("gRPC unavailable"));

            assertThatThrownBy(() -> houseService.getHouseByUserId(keycloakId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Fail to get house by user id");
        }

        @Test
        @DisplayName("should return empty list when user has no houses")
        void getHouseByUserId_noHouses_returnsEmptyList() {
            UserResponse userResponse = UserResponse.newBuilder()
                    .setId(userId.toString())
                    .build();
            given(userClientsGrpc.getUserIdAndRoleByKeyCloakId(keycloakId)).willReturn(userResponse);
            given(houseRepository.findByUserRentalId(userId)).willReturn(List.of());
            given(houseMapper.toDtos(List.of())).willReturn(List.of());

            List<HouseDto> result = houseService.getHouseByUserId(keycloakId);

            assertThat(result).isEmpty();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // uploadHouseImages
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("uploadHouseImages")
    class UploadHouseImagesTests {

        @Test
        @DisplayName("should upload each file and save image record")
        void uploadHouseImages_success() {
            given(houseRepository.existsById(houseId)).willReturn(true);
            given(houseRepository.getReferenceById(houseId)).willReturn(house);

            MultipartFile file1 = mock(MultipartFile.class);
            MultipartFile file2 = mock(MultipartFile.class);
            given(s3.upload(file1, "houses/" + houseId)).willReturn("key1");
            given(s3.upload(file2, "houses/" + houseId)).willReturn("key2");

            houseService.uploadHouseImages(houseId, List.of(file1, file2));

            verify(houseImageRepository, times(2)).save(any(HouseImage.class));
            verify(s3).upload(file1, "houses/" + houseId);
            verify(s3).upload(file2, "houses/" + houseId);
        }

        @Test
        @DisplayName("should throw NotFoundException when house does not exist")
        void uploadHouseImages_houseNotFound_throwsNotFoundException() {
            given(houseRepository.existsById(houseId)).willReturn(false);

            assertThatThrownBy(() -> houseService.uploadHouseImages(houseId, List.of()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("House not found");

            verifyNoInteractions(s3, houseImageRepository);
        }

        @Test
        @DisplayName("should save image with correct key and house reference")
        void uploadHouseImages_imageEntityHasCorrectKey() {
            given(houseRepository.existsById(houseId)).willReturn(true);
            given(houseRepository.getReferenceById(houseId)).willReturn(house);
            MultipartFile file = mock(MultipartFile.class);
            given(s3.upload(file, "houses/" + houseId)).willReturn("s3-key-123");

            houseService.uploadHouseImages(houseId, List.of(file));

            ArgumentCaptor<HouseImage> captor = ArgumentCaptor.forClass(HouseImage.class);
            verify(houseImageRepository).save(captor.capture());
            assertThat(captor.getValue().getKey()).isEqualTo("s3-key-123");
            assertThat(captor.getValue().getHouse()).isEqualTo(house);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // deleteHouseImage
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteHouseImage")
    class DeleteHouseImageTests {

        @Test
        @DisplayName("should delete s3 object and image record")
        void deleteHouseImage_success() {
            HouseImage image = HouseImage.builder()
                    .id(imageId)
                    .house(house)
                    .key("some-s3-key")
                    .build();
            given(houseImageRepository.findById(imageId)).willReturn(Optional.of(image));

            houseService.deleteHouseImage(houseId, imageId);

            verify(s3).delete("some-s3-key");
            verify(houseImageRepository).delete(image);
        }

        @Test
        @DisplayName("should throw NotFoundException when image does not exist")
        void deleteHouseImage_imageNotFound_throwsNotFoundException() {
            given(houseImageRepository.findById(imageId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> houseService.deleteHouseImage(houseId, imageId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("House image not found");

            verifyNoInteractions(s3);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // activeHouseForUser
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("activeHouseForUser")
    class ActiveHouseForUserTests {

        @Test
        @DisplayName("should throw NotFoundException when house not found")
        void activeHouseForUser_houseNotFound_throwsNotFoundException() {
            given(houseRepository.findById(houseId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> houseService.activeHouseForUser(userId, houseId, Instant.now()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("House not found");
        }

        @Test
        @DisplayName("should queue next tenant when house currently occupied and handoverDate is future")
        void activeHouseForUser_currentlyOccupied_setsPendingNextTenant() {
            Instant futureHandover = Instant.now().plusSeconds(3600);

            // House is occupied: userRentalId set, RENTED, handoverDate within buffer
            house.setUserRentalId(UUID.randomUUID());
            house.setStatus(HouseStatus.RENTED);
            house.setHandoverDate(Instant.now().plusSeconds(7200)); // future → within buffer

            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));

            // createPendingTenantGroup: no inactive group → create new
            TenantGroup pendingGroup = TenantGroup.builder()
                    .id(UUID.randomUUID())
                    .houseId(houseId)
                    .isActive(false)
                    .build();
            given(tenantGroupRepository.findAllByHouseId(houseId)).willReturn(List.of());
            given(tenantGroupRepository.save(any(TenantGroup.class))).willReturn(pendingGroup);

            TenantMemberId memberId = new TenantMemberId();
            memberId.setTenantId(pendingGroup.getId());
            memberId.setUserId(userId);
            given(tenantMemberRepository.existsById(any(TenantMemberId.class))).willReturn(false);

            houseService.activeHouseForUser(userId, houseId, futureHandover);

            ArgumentCaptor<House> houseCaptor = ArgumentCaptor.forClass(House.class);
            verify(houseRepository).save(houseCaptor.capture());
            assertThat(houseCaptor.getValue().getNextTenantId()).isEqualTo(userId);
            assertThat(houseCaptor.getValue().getNextHandoverDate()).isEqualTo(futureHandover);
            verify(tenantMemberRepository).save(any(TenantMember.class));
        }

        @Test
        @DisplayName("should reuse existing inactive TenantGroup when creating pending tenant")
        void activeHouseForUser_existingInactiveTenantGroup_reused() {
            Instant futureHandover = Instant.now().plusSeconds(3600);
            house.setUserRentalId(UUID.randomUUID());
            house.setStatus(HouseStatus.RENTED);
            house.setHandoverDate(Instant.now().plusSeconds(7200));

            TenantGroup existingInactive = TenantGroup.builder()
                    .id(UUID.randomUUID())
                    .houseId(houseId)
                    .isActive(false)
                    .build();

            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));
            given(tenantGroupRepository.findAllByHouseId(houseId)).willReturn(List.of(existingInactive));
            given(tenantMemberRepository.existsById(any(TenantMemberId.class))).willReturn(false);

            houseService.activeHouseForUser(userId, houseId, futureHandover);

            // Should NOT create a new group — reuses the existing inactive one
            verify(tenantGroupRepository, never()).save(any(TenantGroup.class));
            verify(tenantMemberRepository).save(any(TenantMember.class));
        }

        @Test
        @DisplayName("should not duplicate TenantMember if member already exists (pending path)")
        void activeHouseForUser_pendingPath_memberAlreadyExists_nodup() {
            Instant futureHandover = Instant.now().plusSeconds(3600);
            house.setUserRentalId(UUID.randomUUID());
            house.setStatus(HouseStatus.RENTED);
            house.setHandoverDate(Instant.now().plusSeconds(7200));

            TenantGroup existingInactive = TenantGroup.builder()
                    .id(UUID.randomUUID())
                    .houseId(houseId)
                    .isActive(false)
                    .build();

            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));
            given(tenantGroupRepository.findAllByHouseId(houseId)).willReturn(List.of(existingInactive));
            given(tenantMemberRepository.existsById(any(TenantMemberId.class))).willReturn(true);

            houseService.activeHouseForUser(userId, houseId, futureHandover);

            verify(tenantMemberRepository, never()).save(any(TenantMember.class));
        }

        @Test
        @DisplayName("should activateNow when house is not currently occupied")
        void activeHouseForUser_notOccupied_activatesNow() {
            Instant handoverDate = Instant.now().plusSeconds(86400);

            // House has no current tenant
            house.setUserRentalId(null);
            house.setStatus(HouseStatus.AVAILABLE);
            house.setHandoverDate(null);
            house.setTenantGroupId(null);

            UUID groupId = UUID.randomUUID();
            TenantGroup activeGroup = TenantGroup.builder()
                    .id(groupId)
                    .houseId(houseId)
                    .isActive(true)
                    .build();

            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));
            given(tenantGroupRepository.findAllByHouseIdAndIsActiveTrue(houseId))
                    .willReturn(List.of());
            given(tenantGroupRepository.save(any(TenantGroup.class))).willReturn(activeGroup);
            given(tenantMemberRepository.existsById(any(TenantMemberId.class))).willReturn(false);

            houseService.activeHouseForUser(userId, houseId, handoverDate);

            ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
            verify(houseRepository).save(captor.capture());
            House saved = captor.getValue();
            assertThat(saved.getUserRentalId()).isEqualTo(userId);
            assertThat(saved.getStatus()).isEqualTo(HouseStatus.RENTED);
            assertThat(saved.getHandoverDate()).isEqualTo(handoverDate);
            assertThat(saved.getNextTenantId()).isNull();
            assertThat(saved.getNextHandoverDate()).isNull();
        }

        @Test
        @DisplayName("should heal duplicate active TenantGroups instead of blocking activation")
        void activeHouseForUser_duplicateActiveTenantGroups_healsAndActivates() {
            house.setUserRentalId(null);
            house.setStatus(HouseStatus.AVAILABLE);
            house.setHandoverDate(null);
            house.setTenantGroupId(null);

            TenantGroup duplicateOne = TenantGroup.builder()
                    .id(UUID.randomUUID()).houseId(houseId).isActive(true).build();
            TenantGroup duplicateTwo = TenantGroup.builder()
                    .id(UUID.randomUUID()).houseId(houseId).isActive(true).build();
            TenantGroup newGroup = TenantGroup.builder()
                    .id(UUID.randomUUID()).houseId(houseId).isActive(true).build();

            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));
            given(tenantGroupRepository.findAllByHouseIdAndIsActiveTrue(houseId))
                    .willReturn(List.of(duplicateOne, duplicateTwo));
            given(tenantMemberRepository.existsByTenantGroupIdAndUserId(any(), eq(userId)))
                    .willReturn(false);
            given(tenantGroupRepository.save(any(TenantGroup.class))).willReturn(newGroup);
            given(tenantMemberRepository.existsById(any(TenantMemberId.class))).willReturn(false);

            houseService.activeHouseForUser(userId, houseId, Instant.now());

            assertThat(duplicateOne.isActive()).isFalse();
            assertThat(duplicateTwo.isActive()).isFalse();
            assertThat(house.getTenantGroupId()).isEqualTo(newGroup.getId());
            assertThat(house.getUserRentalId()).isEqualTo(userId);
            assertThat(house.getStatus()).isEqualTo(HouseStatus.RENTED);
        }

        @Test
        @DisplayName("should deactivate old TenantGroup when house already has tenantGroupId on activateNow")
        void activeHouseForUser_activateNow_deactivatesOldGroup() {
            UUID oldGroupId = UUID.randomUUID();
            house.setUserRentalId(null);
            house.setStatus(HouseStatus.AVAILABLE);
            house.setHandoverDate(null);
            house.setTenantGroupId(oldGroupId);

            TenantGroup oldGroup = TenantGroup.builder()
                    .id(oldGroupId).houseId(houseId).isActive(true).build();

            UUID newGroupId = UUID.randomUUID();
            TenantGroup newGroup = TenantGroup.builder()
                    .id(newGroupId).houseId(houseId).isActive(true).build();

            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));
            given(tenantGroupRepository.findById(oldGroupId)).willReturn(Optional.of(oldGroup));
            given(tenantGroupRepository.findAllByHouseIdAndIsActiveTrue(houseId))
                    .willReturn(List.of());
            given(tenantGroupRepository.save(any(TenantGroup.class))).willReturn(newGroup);
            given(tenantMemberRepository.existsById(any(TenantMemberId.class))).willReturn(false);

            houseService.activeHouseForUser(userId, houseId, Instant.now());

            ArgumentCaptor<TenantGroup> groupCaptor = ArgumentCaptor.forClass(TenantGroup.class);
            verify(tenantGroupRepository, atLeastOnce()).save(groupCaptor.capture());
            // First save should deactivate the old group
            TenantGroup deactivated = groupCaptor.getAllValues().get(0);
            assertThat(deactivated.isActive()).isFalse();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getMyHouseAccess
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getMyHouseAccess")
    class GetMyHouseAccessTests {

        private final String keycloakId = "kc-id-xyz";
        private UUID invoiceId;

        @BeforeEach
        void setUp() {
            invoiceId = UUID.randomUUID();
            UserResponse userResponse = UserResponse.newBuilder()
                    .setId(userId.toString())
                    .build();
            given(userClientsGrpc.getUserIdAndRoleByKeyCloakId(keycloakId)).willReturn(userResponse);
        }

        private void givenSingleHouse(House h) {
            given(houseRepository.findAccessibleByUserId(userId)).willReturn(List.of(h));
        }

        @Test
        @DisplayName("should return PENDING_DEPOSIT when deposit not paid")
        void getMyHouseAccess_depositNotPaid_returnsPendingDeposit() {
            house.setUserRentalId(userId);
            house.setHandoverDate(Instant.now().minusSeconds(3600)); // past
            givenSingleHouse(house);

            given(paymentGrpcClient.getInvoiceStatus(houseId, userId))
                    .willReturn(new InvoiceStatusDto(false, false, invoiceId));

            List<HouseAccessStatus> result = houseService.getMyHouseAccess(keycloakId);

            assertThat(result).hasSize(1);
            HouseAccessStatus status = result.get(0);
            assertThat(status.accessStatus()).isEqualTo(AccessStatus.PENDING_DEPOSIT);
            assertThat(status.reason()).isEqualTo("ACCESS_PENDING_DEPOSIT");
            assertThat(status.hasUnpaidInvoice()).isTrue();
        }

        @Test
        @DisplayName("should return PENDING_HANDOVER when user is pending next tenant and nextHandoverDate is future")
        void getMyHouseAccess_pendingNextTenant_returnsPendingHandover() {
            Instant futureNext = Instant.now().plusSeconds(3600);
            house.setNextTenantId(userId);             // user is pending next
            house.setNextHandoverDate(futureNext);
            house.setUserRentalId(UUID.randomUUID());  // someone else is current owner
            givenSingleHouse(house);

            given(paymentGrpcClient.getInvoiceStatus(houseId, userId))
                    .willReturn(new InvoiceStatusDto(true, true, null));

            List<HouseAccessStatus> result = houseService.getMyHouseAccess(keycloakId);

            HouseAccessStatus status = result.get(0);
            assertThat(status.accessStatus()).isEqualTo(AccessStatus.PENDING_HANDOVER);
            assertThat(status.reason()).isEqualTo("ACCESS_PENDING_HANDOVER");
            assertThat(status.handoverDate()).isEqualTo(futureNext); // uses nextHandoverDate
            assertThat(status.memberRole()).isEqualTo(HouseMemberRole.MEMBER);
        }

        @Test
        @DisplayName("should return PENDING_HANDOVER when current handoverDate is in the future")
        void getMyHouseAccess_handoverDateInFuture_returnsPendingHandover() {
            Instant futureHandover = Instant.now().plusSeconds(3600);
            house.setUserRentalId(userId);
            house.setHandoverDate(futureHandover);
            house.setNextTenantId(null);
            givenSingleHouse(house);

            given(paymentGrpcClient.getInvoiceStatus(houseId, userId))
                    .willReturn(new InvoiceStatusDto(true, true, null));

            List<HouseAccessStatus> result = houseService.getMyHouseAccess(keycloakId);

            HouseAccessStatus status = result.get(0);
            assertThat(status.accessStatus()).isEqualTo(AccessStatus.PENDING_HANDOVER);
            assertThat(status.handoverDate()).isEqualTo(futureHandover);
        }

        @Test
        @DisplayName("should return PENDING_FIRST_RENT when deposit paid but first rent not paid")
        void getMyHouseAccess_firstRentNotPaid_returnsPendingFirstRent() {
            house.setUserRentalId(userId);
            house.setHandoverDate(Instant.now().minusSeconds(3600)); // past
            house.setNextTenantId(null);
            givenSingleHouse(house);

            given(paymentGrpcClient.getInvoiceStatus(houseId, userId))
                    .willReturn(new InvoiceStatusDto(true, false, invoiceId));

            List<HouseAccessStatus> result = houseService.getMyHouseAccess(keycloakId);

            HouseAccessStatus status = result.get(0);
            assertThat(status.accessStatus()).isEqualTo(AccessStatus.PENDING_FIRST_RENT);
            assertThat(status.reason()).isEqualTo("ACCESS_PENDING_FIRST_RENT");
        }

        @Test
        @DisplayName("should return PAYMENT_RESTRICTED when all paid but house is payment-restricted")
        void getMyHouseAccess_paymentRestricted_returnsPaymentRestricted() {
            house.setUserRentalId(userId);
            house.setHandoverDate(Instant.now().minusSeconds(3600)); // past
            house.setNextTenantId(null);
            house.setPaymentRestricted(true);
            givenSingleHouse(house);

            given(paymentGrpcClient.getInvoiceStatus(houseId, userId))
                    .willReturn(new InvoiceStatusDto(true, true, null));

            List<HouseAccessStatus> result = houseService.getMyHouseAccess(keycloakId);

            HouseAccessStatus status = result.get(0);
            assertThat(status.accessStatus()).isEqualTo(AccessStatus.PAYMENT_RESTRICTED);
            assertThat(status.reason()).isEqualTo("PAYMENT_RESTRICTED");
        }

        @Test
        @DisplayName("should return ACCESSIBLE when all conditions cleared")
        void getMyHouseAccess_allClear_returnsAccessible() {
            house.setUserRentalId(userId);
            house.setHandoverDate(Instant.now().minusSeconds(3600)); // past
            house.setNextTenantId(null);
            house.setPaymentRestricted(false);
            givenSingleHouse(house);

            given(paymentGrpcClient.getInvoiceStatus(houseId, userId))
                    .willReturn(new InvoiceStatusDto(true, true, null));

            List<HouseAccessStatus> result = houseService.getMyHouseAccess(keycloakId);

            HouseAccessStatus status = result.get(0);
            assertThat(status.accessStatus()).isEqualTo(AccessStatus.ACCESSIBLE);
            assertThat(status.reason()).isNull();
            assertThat(status.hasUnpaidInvoice()).isFalse();
        }

        @Test
        @DisplayName("should assign OWNER role when userId matches userRentalId")
        void getMyHouseAccess_ownerRole() {
            house.setUserRentalId(userId);
            house.setHandoverDate(Instant.now().minusSeconds(3600));
            house.setNextTenantId(null);
            house.setPaymentRestricted(false);
            givenSingleHouse(house);

            given(paymentGrpcClient.getInvoiceStatus(houseId, userId))
                    .willReturn(new InvoiceStatusDto(true, true, null));

            List<HouseAccessStatus> result = houseService.getMyHouseAccess(keycloakId);

            assertThat(result.get(0).memberRole()).isEqualTo(HouseMemberRole.OWNER);
        }

        @Test
        @DisplayName("should assign MEMBER role when userId does not match userRentalId")
        void getMyHouseAccess_memberRole() {
            house.setUserRentalId(UUID.randomUUID()); // different user is owner
            house.setHandoverDate(Instant.now().minusSeconds(3600));
            house.setNextTenantId(null);
            house.setPaymentRestricted(false);
            givenSingleHouse(house);

            given(paymentGrpcClient.getInvoiceStatus(houseId, userId))
                    .willReturn(new InvoiceStatusDto(true, true, null));

            List<HouseAccessStatus> result = houseService.getMyHouseAccess(keycloakId);

            assertThat(result.get(0).memberRole()).isEqualTo(HouseMemberRole.MEMBER);
        }

        @Test
        @DisplayName("should return empty list when user has no accessible houses")
        void getMyHouseAccess_noHouses_returnsEmptyList() {
            given(houseRepository.findAccessibleByUserId(userId)).willReturn(List.of());

            List<HouseAccessStatus> result = houseService.getMyHouseAccess(keycloakId);

            assertThat(result).isEmpty();
            verifyNoInteractions(paymentGrpcClient);
        }

        @Test
        @DisplayName("PENDING_DEPOSIT takes priority over PENDING_HANDOVER")
        void getMyHouseAccess_depositPriorityOverHandover() {
            // handoverDate is in future (would be PENDING_HANDOVER), but deposit not paid
            house.setUserRentalId(userId);
            house.setHandoverDate(Instant.now().plusSeconds(3600));
            house.setNextTenantId(null);
            givenSingleHouse(house);

            given(paymentGrpcClient.getInvoiceStatus(houseId, userId))
                    .willReturn(new InvoiceStatusDto(false, false, invoiceId));

            List<HouseAccessStatus> result = houseService.getMyHouseAccess(keycloakId);

            assertThat(result.get(0).accessStatus()).isEqualTo(AccessStatus.PENDING_DEPOSIT);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // deactivateHouseForUser
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deactivateHouseForUser")
    class DeactivateHouseForUserTests {

        @Test
        @DisplayName("should clear tenant fields and set AVAILABLE when tenant matches")
        void deactivateHouseForUser_success() {
            house.setUserRentalId(userId);
            house.setTenantGroupId(UUID.randomUUID());
            house.setHandoverDate(Instant.now());
            house.setNextTenantId(userId);
            house.setNextHandoverDate(Instant.now().plusSeconds(3600));
            house.setStatus(HouseStatus.RENTED);

            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));
            given(tenantGroupRepository.findAllByHouseIdAndIsActiveTrue(houseId)).willReturn(List.of());

            houseService.deactivateHouseForUser(userId, houseId, false);

            ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
            verify(houseRepository).save(captor.capture());
            House saved = captor.getValue();
            assertThat(saved.getUserRentalId()).isNull();
            assertThat(saved.getTenantGroupId()).isNull();
            assertThat(saved.getHandoverDate()).isNull();
            assertThat(saved.getNextTenantId()).isNull();
            assertThat(saved.getNextHandoverDate()).isNull();
            assertThat(saved.getStatus()).isEqualTo(HouseStatus.AVAILABLE);
        }

        @Test
        @DisplayName("should preserve pending next tenant when it belongs to another user")
        void deactivateHouseForUser_preservesOtherNextTenant() {
            UUID otherTenantId = UUID.randomUUID();
            Instant nextHandover = Instant.now().plusSeconds(3600);
            house.setUserRentalId(userId);
            house.setNextTenantId(otherTenantId);
            house.setNextHandoverDate(nextHandover);
            house.setStatus(HouseStatus.RENTED);

            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));
            given(tenantGroupRepository.findAllByHouseIdAndIsActiveTrue(houseId)).willReturn(List.of());

            houseService.deactivateHouseForUser(userId, houseId, false);

            ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
            verify(houseRepository).save(captor.capture());
            House saved = captor.getValue();
            assertThat(saved.getNextTenantId()).isEqualTo(otherTenantId);
            assertThat(saved.getNextHandoverDate()).isEqualTo(nextHandover);
        }

        @Test
        @DisplayName("should keep house unavailable (REPAIRED) when keepUnavailable=true")
        void deactivateHouseForUser_keepUnavailable_setsRepaired() {
            house.setUserRentalId(userId);
            house.setStatus(HouseStatus.RENTED);
            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));
            given(tenantGroupRepository.findAllByHouseIdAndIsActiveTrue(houseId)).willReturn(List.of());

            houseService.deactivateHouseForUser(userId, houseId, true);

            ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
            verify(houseRepository).save(captor.capture());
            House saved = captor.getValue();
            assertThat(saved.getUserRentalId()).isNull();
            assertThat(saved.getStatus()).isEqualTo(HouseStatus.REPAIRED);
        }

        @Test
        @DisplayName("should do nothing when tenantId does not match userRentalId")
        void deactivateHouseForUser_tenantMismatch_noSave() {
            house.setUserRentalId(UUID.randomUUID()); // different tenant
            house.setStatus(HouseStatus.RENTED);

            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));

            houseService.deactivateHouseForUser(userId, houseId, false);

            verify(houseRepository, never()).save(any(House.class));
        }

        @Test
        @DisplayName("should throw NotFoundException when house not found")
        void deactivateHouseForUser_houseNotFound_throwsNotFoundException() {
            given(houseRepository.findById(houseId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> houseService.deactivateHouseForUser(userId, houseId, false))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("House not found");

            verify(houseRepository, never()).save(any());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // setTenantAccessRestriction
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("setTenantAccessRestriction")
    class SetTenantAccessRestrictionTests {

        @Test
        @DisplayName("should set paymentRestricted=true when tenant matches")
        void setTenantAccessRestriction_restrict_success() {
            house.setUserRentalId(userId);
            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));

            houseService.setTenantAccessRestriction(userId, houseId, true);

            ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
            verify(houseRepository).save(captor.capture());
            assertThat(captor.getValue().getPaymentRestricted()).isTrue();
        }

        @Test
        @DisplayName("should set paymentRestricted=false when lifting restriction")
        void setTenantAccessRestriction_unrestrict_success() {
            house.setUserRentalId(userId);
            house.setPaymentRestricted(true);
            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));

            houseService.setTenantAccessRestriction(userId, houseId, false);

            ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
            verify(houseRepository).save(captor.capture());
            assertThat(captor.getValue().getPaymentRestricted()).isFalse();
        }

        @Test
        @DisplayName("should do nothing when tenantId does not match userRentalId")
        void setTenantAccessRestriction_tenantMismatch_noSave() {
            house.setUserRentalId(UUID.randomUUID());
            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));

            houseService.setTenantAccessRestriction(userId, houseId, true);

            verify(houseRepository, never()).save(any(House.class));
        }

        @Test
        @DisplayName("should throw NotFoundException when house not found")
        void setTenantAccessRestriction_houseNotFound_throwsNotFoundException() {
            given(houseRepository.findById(houseId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> houseService.setTenantAccessRestriction(userId, houseId, true))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("House not found");

            verify(houseRepository, never()).save(any());
        }

        @Test
        @DisplayName("should update updatedAt timestamp on successful restriction change")
        void setTenantAccessRestriction_updatesTimestamp() {
            Instant before = Instant.now().minusSeconds(10);
            house.setUserRentalId(userId);
            house.setUpdatedAt(before);
            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));

            houseService.setTenantAccessRestriction(userId, houseId, true);

            ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
            verify(houseRepository).save(captor.capture());
            assertThat(captor.getValue().getUpdatedAt()).isAfterOrEqualTo(before);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getHousesByRegionId
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getHousesByRegionId")
    class GetHousesByRegionIdTests {

        @Test
        @DisplayName("should return list of dtos for the given region")
        void getHousesByRegionId_success() {
            given(houseRepository.findAllByRegionId(regionId)).willReturn(List.of(house));
            given(houseMapper.toDto(house)).willReturn(houseDto);

            List<HouseDto> result = houseService.getHousesByRegionId(regionId);

            assertThat(result).containsExactly(houseDto);
        }

        @Test
        @DisplayName("should return empty list when no houses in region")
        void getHousesByRegionId_empty_returnsEmptyList() {
            given(houseRepository.findAllByRegionId(regionId)).willReturn(List.of());

            List<HouseDto> result = houseService.getHousesByRegionId(regionId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should wrap exception in RuntimeException")
        void getHousesByRegionId_repositoryThrows_wrapsException() {
            given(houseRepository.findAllByRegionId(regionId))
                    .willThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> houseService.getHousesByRegionId(regionId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Cannot get houses by region");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getAll (pagination)
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAll (pagination)")
    class GetAllTests {

        @Test
        @DisplayName("should delegate to cachedPageService and return result")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void getAll_delegatesToCachedPageService() {
            PageRequest request = mock(PageRequest.class);
            PageResponse expected = mock(PageResponse.class);

            // Use doReturn to avoid generic type mismatch at compile time
            doReturn(expected).when(cachedPageService)
                    .getOrLoad(startsWith("houses"), eq(request), any(), any());

            PageResponse<HouseDto> result = houseService.getAll(request);

            assertThat(result).isSameAs(expected);
        }

        @Test
        @DisplayName("should build local spec and filter houseId by entity id")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void getAll_filtersHouseIdByEntityId() {
            PageRequest request = PageRequest.builder()
                    .page(0)
                    .size(10)
                    .keyword("Test")
                    .filters(Map.of(
                            "status", "available",
                            "statuses", "AVAILABLE,INACTIVE",
                            "houseId", houseId.toString()
                    ))
                    .build();

            ArgumentCaptor<Specification<House>> specCaptor = ArgumentCaptor.forClass(Specification.class);

            doAnswer(invocation -> invocation.<java.util.function.Supplier<PageResponse<HouseDto>>>getArgument(3).get())
                    .when(cachedPageService)
                    .getOrLoad(startsWith("houses"), eq(request), any(), any());

            given(houseRepository.findAll(specCaptor.capture(), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(house)));
            given(houseMapper.toDto(house)).willReturn(houseDto);
            given(houseImageRepository.findByHouseId(houseId)).willReturn(List.of());

            PageResponse<HouseDto> result = houseService.getAll(request);

            assertThat(result.items()).containsExactly(houseDto);

            Specification<House> spec = specCaptor.getValue();
            Root<House> root = mock(Root.class);
            CriteriaQuery<?> query = mock(CriteriaQuery.class);
            CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
            Path<String> namePath = mock(Path.class);
            Path<String> addressPath = mock(Path.class);
            Path<Object> statusPath = mock(Path.class);
            Path<Object> idPath = mock(Path.class);
            Expression<String> lowerName = mock(Expression.class);
            Expression<String> lowerAddress = mock(Expression.class);
            Predicate keywordPredicate = mock(Predicate.class);
            Predicate statusPredicate = mock(Predicate.class);
            Predicate statusesPredicate = mock(Predicate.class);
            Predicate idPredicate = mock(Predicate.class);
            Predicate finalPredicate = mock(Predicate.class);

            given(root.get("name")).willReturn((Path) namePath);
            given(root.get("address")).willReturn((Path) addressPath);
            given(root.get("status")).willReturn((Path) statusPath);
            given(root.get("id")).willReturn((Path) idPath);
            given(criteriaBuilder.lower(namePath)).willReturn(lowerName);
            given(criteriaBuilder.lower(addressPath)).willReturn(lowerAddress);
            given(criteriaBuilder.like(lowerName, "%test%")).willReturn(keywordPredicate);
            given(criteriaBuilder.like(lowerAddress, "%test%")).willReturn(keywordPredicate);
            given(criteriaBuilder.or(keywordPredicate, keywordPredicate)).willReturn(keywordPredicate);
            given(criteriaBuilder.equal(statusPath, HouseStatus.AVAILABLE)).willReturn(statusPredicate);
            given(statusPath.in(any(java.util.Collection.class))).willReturn(statusesPredicate);
            given(criteriaBuilder.equal(idPath, houseId)).willReturn(idPredicate);
            given(criteriaBuilder.and(any(Predicate[].class))).willReturn(finalPredicate);

            Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);

            assertThat(predicate).isSameAs(finalPredicate);
            verify(root).get("id");
            verify(root, never()).get("houseId");
            verify(criteriaBuilder).equal(idPath, houseId);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // updateHouseTranslations
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateHouseTranslations")
    class UpdateHouseTranslationsTests {

        @BeforeEach
        void seedExistingTranslations() {
            // Start each test with full vi/en/ja for every translatable field
            house.setNameTranslations(TranslationMap.of(Map.of(
                    "vi", "Nhà Hiệu Quận 7",
                    "en", "Hieu House District 7",
                    "ja", "ヒューの家 7区"
            )));
            house.setAddressTranslations(TranslationMap.of(Map.of(
                    "vi", "123 Nguyễn Văn Linh",
                    "en", "123 Nguyen Van Linh",
                    "ja", "123 グエンヴァンリン"
            )));
            house.setWardTranslations(TranslationMap.of(Map.of(
                    "vi", "Phường Tân Phong", "en", "Tan Phong Ward", "ja", "タンフォン町"
            )));
            house.setCommuneTranslations(TranslationMap.of(Map.of(
                    "vi", "", "en", "", "ja", ""
            )));
            house.setCityTranslations(TranslationMap.of(Map.of(
                    "vi", "TP. Hồ Chí Minh", "en", "Ho Chi Minh City", "ja", "ホーチミン市"
            )));
            house.setDescriptionTranslations(TranslationMap.of(Map.of(
                    "vi", "Nhà 3 tầng view sông",
                    "en", "3-floor house with river view",
                    "ja", "3階建ての家、川ビュー"
            )));
            // Base Vietnamese columns match the 'vi' value in each TranslationMap
            house.setName("Nhà Hiệu Quận 7");
            house.setAddress("123 Nguyễn Văn Linh");
            house.setWard("Phường Tân Phong");
            house.setCommune("");
            house.setCity("TP. Hồ Chí Minh");
            house.setDescription("Nhà 3 tầng view sông");
        }

        @Test
        @DisplayName("throws NotFoundException when house not found")
        void notFound_throws() {
            given(houseRepository.findById(houseId)).willReturn(Optional.empty());

            UpdateHouseTranslationsRequest req = new UpdateHouseTranslationsRequest(
                    Map.of("ja", "xxx"), null, null, null, null, null
            );

            assertThatThrownBy(() -> houseService.updateHouseTranslations(houseId, req))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("House not found");

            verify(houseRepository, never()).save(any());
            verifyNoInteractions(cachedPageService);
        }

        @Test
        @DisplayName("patch only description-ja: keeps all other locales & fields unchanged")
        void patchOnlyJaDescription_preservesOthers() {
            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));
            given(houseRepository.save(any(House.class))).willAnswer(inv -> inv.getArgument(0));
            given(houseMapper.toDto(any(House.class))).willReturn(houseDto);

            UpdateHouseTranslationsRequest req = new UpdateHouseTranslationsRequest(
                    null, null, null, null, null,
                    Map.of("ja", "3階建て、川ビューの家（訂正版）")
            );

            houseService.updateHouseTranslations(houseId, req);

            ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
            verify(houseRepository).save(captor.capture());
            House saved = captor.getValue();

            // description: only JA replaced, VI+EN preserved
            Map<String, String> desc = saved.getDescriptionTranslations().getTranslations();
            assertThat(desc).containsEntry("ja", "3階建て、川ビューの家（訂正版）");
            assertThat(desc).containsEntry("vi", "Nhà 3 tầng view sông");
            assertThat(desc).containsEntry("en", "3-floor house with river view");

            // Base VI column of description not touched (patch has no 'vi' key)
            assertThat(saved.getDescription()).isEqualTo("Nhà 3 tầng view sông");

            // Other fields completely untouched
            assertThat(saved.getNameTranslations().getTranslations())
                    .containsEntry("ja", "ヒューの家 7区");
            assertThat(saved.getAddressTranslations().getTranslations())
                    .containsEntry("en", "123 Nguyen Van Linh");
            assertThat(saved.getCityTranslations().getTranslations())
                    .containsEntry("ja", "ホーチミン市");
        }

        @Test
        @DisplayName("patch containing vi syncs base column in addition to translation map")
        void patchWithVi_syncsBaseColumn() {
            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));
            given(houseRepository.save(any(House.class))).willAnswer(inv -> inv.getArgument(0));
            given(houseMapper.toDto(any(House.class))).willReturn(houseDto);

            UpdateHouseTranslationsRequest req = new UpdateHouseTranslationsRequest(
                    Map.of("vi", "Nhà Hiệu Q7 (sửa)"),
                    null, null, null, null, null
            );

            houseService.updateHouseTranslations(houseId, req);

            ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
            verify(houseRepository).save(captor.capture());
            House saved = captor.getValue();

            assertThat(saved.getName()).isEqualTo("Nhà Hiệu Q7 (sửa)");
            assertThat(saved.getNameTranslations().getTranslations())
                    .containsEntry("vi", "Nhà Hiệu Q7 (sửa)")
                    .containsEntry("en", "Hieu House District 7")
                    .containsEntry("ja", "ヒューの家 7区");
        }

        @Test
        @DisplayName("patch multiple fields at once merges each independently")
        void patchMultipleFields_merged() {
            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));
            given(houseRepository.save(any(House.class))).willAnswer(inv -> inv.getArgument(0));
            given(houseMapper.toDto(any(House.class))).willReturn(houseDto);

            UpdateHouseTranslationsRequest req = new UpdateHouseTranslationsRequest(
                    Map.of("en", "Hieu Mansion D7"),
                    null, null, null,
                    Map.of("ja", "HCM市 (訂正)"),
                    Map.of("en", "Three-storey river-view house")
            );

            houseService.updateHouseTranslations(houseId, req);

            ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
            verify(houseRepository).save(captor.capture());
            House saved = captor.getValue();

            assertThat(saved.getNameTranslations().getTranslations())
                    .containsEntry("en", "Hieu Mansion D7")
                    .containsEntry("vi", "Nhà Hiệu Quận 7")
                    .containsEntry("ja", "ヒューの家 7区");
            assertThat(saved.getCityTranslations().getTranslations())
                    .containsEntry("ja", "HCM市 (訂正)")
                    .containsEntry("vi", "TP. Hồ Chí Minh");
            assertThat(saved.getDescriptionTranslations().getTranslations())
                    .containsEntry("en", "Three-storey river-view house")
                    .containsEntry("ja", "3階建ての家、川ビュー");
            // Address/Ward/Commune untouched
            assertThat(saved.getAddressTranslations().getTranslations())
                    .containsEntry("vi", "123 Nguyễn Văn Linh");
        }

        @Test
        @DisplayName("empty / all-null request leaves translations unchanged but saves with bumped updatedAt")
        void emptyRequest_noTranslationChangesButSaves() {
            Instant oldUpdatedAt = Instant.now().minusSeconds(3600);
            house.setUpdatedAt(oldUpdatedAt);

            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));
            given(houseRepository.save(any(House.class))).willAnswer(inv -> inv.getArgument(0));
            given(houseMapper.toDto(any(House.class))).willReturn(houseDto);

            UpdateHouseTranslationsRequest req = new UpdateHouseTranslationsRequest(
                    null, null, null, null, null, null
            );

            houseService.updateHouseTranslations(houseId, req);

            ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
            verify(houseRepository).save(captor.capture());
            House saved = captor.getValue();

            // All translations preserved as-is
            assertThat(saved.getNameTranslations().getTranslations())
                    .containsEntry("vi", "Nhà Hiệu Quận 7")
                    .containsEntry("en", "Hieu House District 7")
                    .containsEntry("ja", "ヒューの家 7区");
            // Base VI columns preserved
            assertThat(saved.getName()).isEqualTo("Nhà Hiệu Quận 7");
            // updatedAt moved forward
            assertThat(saved.getUpdatedAt()).isAfter(oldUpdatedAt);
        }

        @Test
        @DisplayName("blank / null values in patch are skipped (existing value retained)")
        void blankValuesInPatch_skipped() {
            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));
            given(houseRepository.save(any(House.class))).willAnswer(inv -> inv.getArgument(0));
            given(houseMapper.toDto(any(House.class))).willReturn(houseDto);

            // Java Map.of() disallows null values, so use a mutable map for the null-value case
            Map<String, String> patch = new java.util.HashMap<>();
            patch.put("en", "   ");   // blank → skipped
            patch.put("ja", null);    // null  → skipped
            patch.put("vi", "Nhà đã sửa");

            UpdateHouseTranslationsRequest req = new UpdateHouseTranslationsRequest(
                    patch, null, null, null, null, null
            );

            houseService.updateHouseTranslations(houseId, req);

            ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
            verify(houseRepository).save(captor.capture());
            House saved = captor.getValue();

            Map<String, String> nameMap = saved.getNameTranslations().getTranslations();
            assertThat(nameMap).containsEntry("vi", "Nhà đã sửa");
            // EN & JA preserved, not overwritten with blank/null
            assertThat(nameMap).containsEntry("en", "Hieu House District 7");
            assertThat(nameMap).containsEntry("ja", "ヒューの家 7区");
            // Base column synced from VI
            assertThat(saved.getName()).isEqualTo("Nhà đã sửa");
        }

        @Test
        @DisplayName("locale keys are normalized to lowercase")
        void localeKeys_normalizedLowercase() {
            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));
            given(houseRepository.save(any(House.class))).willAnswer(inv -> inv.getArgument(0));
            given(houseMapper.toDto(any(House.class))).willReturn(houseDto);

            UpdateHouseTranslationsRequest req = new UpdateHouseTranslationsRequest(
                    Map.of("  JA  ", "大文字JAパッチ"),
                    null, null, null, null, null
            );

            houseService.updateHouseTranslations(houseId, req);

            ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
            verify(houseRepository).save(captor.capture());
            House saved = captor.getValue();

            Map<String, String> nameMap = saved.getNameTranslations().getTranslations();
            assertThat(nameMap).containsEntry("ja", "大文字JAパッチ");
            // Should not have the upper-case or space-padded variant
            assertThat(nameMap).doesNotContainKey("  JA  ");
            assertThat(nameMap).doesNotContainKey("JA");
        }

        @Test
        @DisplayName("adds a new locale when existing TranslationMap does not have it")
        void addsNewLocale() {
            // Seed with only VI (no EN/JA) on the name field
            house.setNameTranslations(TranslationMap.of(Map.of("vi", "Nhà VI only")));
            house.setName("Nhà VI only");

            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));
            given(houseRepository.save(any(House.class))).willAnswer(inv -> inv.getArgument(0));
            given(houseMapper.toDto(any(House.class))).willReturn(houseDto);

            UpdateHouseTranslationsRequest req = new UpdateHouseTranslationsRequest(
                    Map.of("en", "VI-only house", "ja", "VIだけの家"),
                    null, null, null, null, null
            );

            houseService.updateHouseTranslations(houseId, req);

            ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
            verify(houseRepository).save(captor.capture());
            House saved = captor.getValue();

            assertThat(saved.getNameTranslations().getTranslations())
                    .containsEntry("vi", "Nhà VI only")
                    .containsEntry("en", "VI-only house")
                    .containsEntry("ja", "VIだけの家");
            // No 'vi' in patch → base column not touched
            assertThat(saved.getName()).isEqualTo("Nhà VI only");
        }

        @Test
        @DisplayName("evicts pagination cache and returns mapper DTO on success")
        void evictsCacheAndReturnsDto() {
            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));
            House savedEntity = house;
            given(houseRepository.save(any(House.class))).willReturn(savedEntity);
            given(houseMapper.toDto(savedEntity)).willReturn(houseDto);

            UpdateHouseTranslationsRequest req = new UpdateHouseTranslationsRequest(
                    Map.of("ja", "ヒュー邸"), null, null, null, null, null
            );

            HouseDto result = houseService.updateHouseTranslations(houseId, req);

            assertThat(result).isSameAs(houseDto);
            verify(cachedPageService).evictAll("houses");
            verify(houseMapper).toDto(savedEntity);
        }

        @Test
        @DisplayName("never invokes AI auto-fill translation service (manual patch only)")
        void neverInvokesAutoFill() {
            given(houseRepository.findById(houseId)).willReturn(Optional.of(house));
            given(houseRepository.save(any(House.class))).willAnswer(inv -> inv.getArgument(0));
            given(houseMapper.toDto(any(House.class))).willReturn(houseDto);

            UpdateHouseTranslationsRequest req = new UpdateHouseTranslationsRequest(
                    Map.of("ja", "新しい名前"), null, null, null, null, null
            );

            houseService.updateHouseTranslations(houseId, req);

            verify(translationAutoFillService, never()).complete(anyString());
        }
    }
}
