package com.isums.houseservice.services;

import com.isums.houseservice.domains.dtos.*;
import common.i18n.TranslationMap;
import com.isums.houseservice.domains.emuns.AccessStatus;
import com.isums.houseservice.domains.emuns.HouseMemberRole;
import com.isums.houseservice.domains.entities.*;
import com.isums.houseservice.exceptions.NotFoundException;
import com.isums.houseservice.infrastructures.clients.AssetRestClient;
import com.isums.houseservice.infrastructures.clients.HouseHistoryRestClient;
import com.isums.houseservice.infrastructures.abstracts.HouseService;
import com.isums.houseservice.domains.emuns.HouseStatus;
import com.isums.houseservice.infrastructures.grpcs.PaymentGrpcClient;
import com.isums.houseservice.infrastructures.grpcs.UserClientsGrpc;
import com.isums.houseservice.infrastructures.kafkas.HouseEventProducer;
import com.isums.houseservice.infrastructures.kafkas.HouseSubscriptionNotifier;
import com.isums.houseservice.infrastructures.mappers.HouseMapper;
import com.isums.houseservice.infrastructures.repositories.*;
import com.isums.userservice.grpc.UserResponse;
import common.paginations.cache.CachedPageService;
import common.paginations.converters.SpringPageConverter;
import common.paginations.dtos.PageRequest;
import common.paginations.dtos.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HouseServiceImpl implements HouseService {

    private final HouseRepository houseRepository;
    private final HouseMapper houseMapper;
    private final RegionRepository regionRepository;
    private final HouseEventProducer houseEventProducer;
    private final HouseSubscriptionNotifier subscriptionNotifier;
    private final HouseSubscriptionRepository subscriptionRepository;
    private final S3ServiceImpl s3;
    private final HouseImageRepository houseImageRepository;
    private final UserClientsGrpc userClientsGrpc;
    private final AssetRestClient assetRestClient;
    private final HouseHistoryRestClient houseHistoryRestClient;
    private final PaymentGrpcClient paymentGrpcClient;
    private final TenantGroupRepository tenantGroupRepository;
    private final TenantMemberRepository tenantMemberRepository;
    private final CachedPageService cachedPageService;
    private final TranslationAutoFillService translationAutoFillService;

    private static final String PAGE_NS = "houses";

    private static final DateTimeFormatter DMY = DateTimeFormatter
            .ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    @Override
    public HouseDto CreateHouse(CreateHouseRequest req) {
        Region region = regionRepository.findById(req.regionId())
                .orElseThrow(() -> new RuntimeException("Region not found"));

        House house = House.builder()
                .name(req.name())
                .nameTranslations(translationAutoFillService.complete(req.name(), req.nameTranslations()))
                .address(req.address())
                .addressTranslations(translationAutoFillService.complete(req.address(), req.addressTranslations()))
                .ward(req.ward())
                .wardTranslations(translationAutoFillService.complete(req.ward(), req.wardTranslations()))
                .region(region)
                .commune(req.commune())
                .communeTranslations(translationAutoFillService.complete(req.commune(), req.communeTranslations()))
                .city(req.city())
                .cityTranslations(translationAutoFillService.complete(req.city(), req.cityTranslations()))
                .description(req.description())
                .descriptionTranslations(translationAutoFillService.complete(req.description(), req.descriptionTranslations()))
                .numberOfFloors(req.numberOfFloors())
                .paymentRestricted(false)
                .status(HouseStatus.AVAILABLE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        House created = houseRepository.save(house);
        cachedPageService.evictAll(PAGE_NS);
        houseEventProducer.publishHouseCreated(created.getId());
        return houseMapper.toDto(created);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<HouseDto> getAll(PageRequest request) {
        return cachedPageService.getOrLoad(PAGE_NS, request, new TypeReference<>() {
                },
                () -> loadPage(request)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public HouseDto getHouseById(UUID id) {
        try {
            House house = houseRepository.findWithFunctionalAreasById(id)
                    .orElseThrow(() -> new RuntimeException("House not found"));

            HouseDto dto = houseMapper.toDto(house);
            var assetCountByArea = loadAssetCountByArea(id);
            var functionalAreas = dto.functionalAreas() == null
                    ? List.<com.isums.houseservice.domains.dtos.FunctionalAreaDto.FunctionalAreaDto>of()
                    : dto.functionalAreas().stream()
                    .map(area -> new com.isums.houseservice.domains.dtos.FunctionalAreaDto.FunctionalAreaDto(
                            area.id(),
                            area.houseId(),
                            area.name(),
                            area.areaType(),
                            area.floorNo(),
                            area.description(),
                            area.status(),
                            area.createdAt(),
                            area.updatedAt(),
                            assetCountByArea.getOrDefault(area.id(), 0)
                    ))
                    .toList();
            return new HouseDto(
                    dto.id(),
                    dto.userRentalId(),
                    dto.regionId(),
                    dto.name(),
                    dto.address(),
                    dto.ward(),
                    dto.commune(),
                    dto.city(),
                    dto.numberOfFloors(),
                    dto.areaM2(),
                    dto.structure(),
                    dto.landCertNumber(),
                    dto.landCertIssueDate(),
                    dto.landCertIssuer(),
                    dto.paymentRestricted(),
                    dto.description(),
                    dto.status(),
                    functionalAreas,
                    getHouseImages(id),
                    dto.nameTranslations(),
                    dto.addressTranslations(),
                    dto.wardTranslations(),
                    dto.communeTranslations(),
                    dto.cityTranslations(),
                    dto.descriptionTranslations()
            );
        } catch (Exception ex) {
            throw new RuntimeException("Fail to get house by id: " + ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<HouseDto> getHouseByUserId(String userId) {
        try {
            UserResponse user = userClientsGrpc.getUserIdAndRoleByKeyCloakId(userId);
            List<House> houses = houseRepository.findByUserRentalId(UUID.fromString(user.getId()));
            return houseMapper.toDtos(houses);
        } catch (Exception ex) {
            throw new RuntimeException("Fail to get house by user id: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "houseImages", key = "#houseId"),
            @CacheEvict(value = "user-house-access", allEntries = true)
    })
    public void uploadHouseImages(UUID houseId, List<MultipartFile> files) {
        boolean isExist = houseRepository.existsById(houseId);
        if (!isExist) {
            throw new NotFoundException("House not found: " + houseId);
        }

        House house = houseRepository.getReferenceById(houseId);

        files.forEach(file -> {
            String key = s3.upload(file, "houses/" + houseId);

            HouseImage image = HouseImage.builder()
                    .house(house)
                    .key(key)
                    .build();

            houseImageRepository.save(image);
        });
        cachedPageService.evictAll(PAGE_NS);
    }

    private List<HouseImageDto> getHouseImages(UUID houseId) {
        List<HouseImage> images = houseImageRepository.findByHouseId(houseId);

        List<HouseImageDto> imagesDto = new ArrayList<>();
        images.forEach(image -> {
            String url = s3.getImageUrl(image.getKey());
            imagesDto.add(new HouseImageDto(image.getId(), url, image.getCreatedAt()));
        });

        return imagesDto;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "houseImages", key = "#houseId"),
            @CacheEvict(value = "user-house-access", allEntries = true)
    })
    public void deleteHouseImage(UUID houseId, UUID imageId) {
        HouseImage image = houseImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("House image not found"));
        s3.delete(image.getKey());
        houseImageRepository.delete(image);
        cachedPageService.evictAll(PAGE_NS);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "user-house-access", allEntries = true),
            @CacheEvict(value = "marketplace-bookable", allEntries = true),
            @CacheEvict(value = "marketplace-locked", allEntries = true)
    })
    public void activeHouseForUser(UUID userId, UUID houseId, Instant handoverDate) {
        House house = houseRepository.findById(houseId).orElseThrow(() -> new NotFoundException("House not found: " + houseId));

        if (userId.equals(house.getUserRentalId())) {
            if (handoverDate != null) {
                house.setHandoverDate(handoverDate);
            }
            house.setNextTenantId(null);
            house.setNextHandoverDate(null);
            house.setUpdatedAt(Instant.now());
            houseRepository.save(house);
            cachedPageService.evictAll(PAGE_NS);
            houseEventProducer.publishTenantChanged(houseId, userId);
            log.info("[House] Re-activated existing tenant userId={} houseId={} handoverDate={}",
                    userId, houseId, handoverDate);
            return;
        }

        boolean houseCurrentlyOccupied = house.getUserRentalId() != null
                && house.getStatus() == HouseStatus.RENTED;

        if (houseCurrentlyOccupied && handoverDate != null && handoverDate.isAfter(Instant.now())) {
            house.setNextTenantId(userId);
            house.setNextHandoverDate(handoverDate);
            houseRepository.save(house);
            cachedPageService.evictAll(PAGE_NS);

            createPendingTenantGroup(userId, houseId);

            log.info("[House] Pending next tenant userId={} houseId={} handoverDate={}",
                    userId, houseId, handoverDate);
        } else {
            activateNow(house, userId, handoverDate);
        }
    }

    @Override
    @Transactional
    public List<HouseDto> getHousesByRegionId(UUID regionId) {
        try {
            List<House> houses = houseRepository.findAllByRegionId(regionId);

            return houses.stream()
                    .map(houseMapper::toDto)
                    .toList();

        } catch (Exception ex) {
            throw new RuntimeException("Cannot get houses by region: " + ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
//    @Cacheable(value = "user-house-access", key = "#keycloakId")
    public List<HouseAccessStatus> getMyHouseAccess(String keycloakId) {
        UserResponse user = userClientsGrpc.getUserIdAndRoleByKeyCloakId(keycloakId);
        UUID userId = UUID.fromString(user.getId());
        List<House> houses = houseRepository.findAccessibleByUserId(userId);

        Instant now = Instant.now();
        java.util.Map<UUID, InvoiceStatusDto> invoiceByHouse = houses.parallelStream()
                .collect(java.util.stream.Collectors.toConcurrentMap(
                        House::getId,
                        h -> paymentGrpcClient.getInvoiceStatus(h.getId(), userId)));

        return houses.stream().map(house -> {
            boolean isPendingNext = userId.equals(house.getNextTenantId()) && !userId.equals(house.getUserRentalId());

            HouseMemberRole role = userId.equals(house.getUserRentalId())
                    ? HouseMemberRole.OWNER
                    : HouseMemberRole.MEMBER;

            InvoiceStatusDto invoiceStatus = invoiceByHouse.get(house.getId());

            AccessStatus status;
            String reason = null;

            if (!invoiceStatus.depositPaid()) {
                status = AccessStatus.PENDING_DEPOSIT;
                reason = "ACCESS_PENDING_DEPOSIT";
            } else if (isPendingNext && house.getNextHandoverDate() != null
                    && now.isBefore(house.getNextHandoverDate())) {
                status = AccessStatus.PENDING_HANDOVER;
                reason = "ACCESS_PENDING_HANDOVER";
            } else if (house.getHandoverDate() != null && now.isBefore(house.getHandoverDate())) {
                status = AccessStatus.PENDING_HANDOVER;
                reason = "ACCESS_PENDING_HANDOVER";
            } else if (!invoiceStatus.firstRentPaid()) {
                status = AccessStatus.PENDING_FIRST_RENT;
                reason = "ACCESS_PENDING_FIRST_RENT";
            } else if (Boolean.TRUE.equals(house.getPaymentRestricted())) {
                status = AccessStatus.PAYMENT_RESTRICTED;
                reason = "PAYMENT_OVERDUE";
            } else {
                status = AccessStatus.ACCESSIBLE;
            }

            if (Boolean.TRUE.equals(house.getPaymentRestricted())) {
                status = AccessStatus.PAYMENT_RESTRICTED;
                reason = "PAYMENT_RESTRICTED";
            }

                return new HouseAccessStatus(
                        house.getId(),
                        resolveLocalized(house.getName(), house.getNameTranslations()),
                        resolveLocalized(house.getAddress(), house.getAddressTranslations()),
                        isPendingNext ? house.getNextHandoverDate() : house.getHandoverDate(),
                        status,
                        reason,
                        invoiceStatus.pendingInvoiceId() != null,
                        invoiceStatus.pendingInvoiceId(),
                        role
                );
            }).toList();
        }

    @Override
    @Transactional
    @CacheEvict(value = "user-house-access", allEntries = true)
    public HouseDto updateHouseTranslations(UUID houseId, UpdateHouseTranslationsRequest request) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new NotFoundException("House not found: " + houseId));

        house.setNameTranslations(mergeTranslations(house.getNameTranslations(), request.nameTranslations()));
        house.setAddressTranslations(mergeTranslations(house.getAddressTranslations(), request.addressTranslations()));
        house.setWardTranslations(mergeTranslations(house.getWardTranslations(), request.wardTranslations()));
        house.setCommuneTranslations(mergeTranslations(house.getCommuneTranslations(), request.communeTranslations()));
        house.setCityTranslations(mergeTranslations(house.getCityTranslations(), request.cityTranslations()));
        house.setDescriptionTranslations(mergeTranslations(house.getDescriptionTranslations(), request.descriptionTranslations()));

        if (request.nameTranslations() != null && request.nameTranslations().get("vi") != null) {
            house.setName(request.nameTranslations().get("vi"));
        }
        if (request.addressTranslations() != null && request.addressTranslations().get("vi") != null) {
            house.setAddress(request.addressTranslations().get("vi"));
        }
        if (request.wardTranslations() != null && request.wardTranslations().get("vi") != null) {
            house.setWard(request.wardTranslations().get("vi"));
        }
        if (request.communeTranslations() != null && request.communeTranslations().get("vi") != null) {
            house.setCommune(request.communeTranslations().get("vi"));
        }
        if (request.cityTranslations() != null && request.cityTranslations().get("vi") != null) {
            house.setCity(request.cityTranslations().get("vi"));
        }
        if (request.descriptionTranslations() != null && request.descriptionTranslations().get("vi") != null) {
            house.setDescription(request.descriptionTranslations().get("vi"));
        }

        house.setUpdatedAt(Instant.now());
        House saved = houseRepository.save(house);
        cachedPageService.evictAll(PAGE_NS);
        return houseMapper.toDto(saved);
    }

    private TranslationMap mergeTranslations(TranslationMap existing, Map<String, String> patch) {
        if (patch == null || patch.isEmpty()) {
            return existing;
        }
        Map<String, String> merged = new java.util.LinkedHashMap<>();
        if (existing != null && existing.getTranslations() != null) {
            merged.putAll(existing.getTranslations());
        }
        for (Map.Entry<String, String> entry : patch.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) continue;
            if (entry.getValue() == null || entry.getValue().isBlank()) continue;
            merged.put(entry.getKey().trim().toLowerCase(java.util.Locale.ROOT), entry.getValue().trim());
        }
        return TranslationMap.of(merged);
    }

    private String resolveLocalized(String source, common.i18n.TranslationMap translations) {
        if (translations == null || translations.getTranslations().isEmpty()) {
            return source;
        }
        String resolved = translations.resolve();
        return resolved != null && !resolved.isBlank() ? resolved : source;
    }

    @Override
    @Transactional
    @CacheEvict(value = "user-house-access", allEntries = true)
    public void revokeHouseAccessForUser(UUID tenantId, UUID houseId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new NotFoundException("House not found: " + houseId));

        if (house.getUserRentalId() != null && !tenantId.equals(house.getUserRentalId())) {
            log.warn("[House] revokeHouseAccessForUser tenantId={} does not match userRentalId={} houseId={}",
                    tenantId, house.getUserRentalId(), houseId);
            return;
        }

        for (TenantGroup group : tenantGroupRepository.findAllByHouseIdAndIsActiveTrue(houseId)) {
            for (TenantMember m : tenantMemberRepository.findByTenantGroupId(group.getId())) {
                if (m.isActive()) {
                    m.setActive(false);
                    tenantMemberRepository.save(m);
                }
            }
            group.setActive(false);
            tenantGroupRepository.save(group);
            log.info("[House] Revoked tenantGroupId={} houseId={}", group.getId(), houseId);
        }

        boolean hasPendingNextTenant = house.getNextTenantId() != null && !tenantId.equals(house.getNextTenantId());

        house.setUserRentalId(null);
        house.setTenantGroupId(null);
        house.setHandoverDate(null);
        if (tenantId.equals(house.getNextTenantId())) {
            house.setNextTenantId(null);
            house.setNextHandoverDate(null);
        }
        house.setStatus(hasPendingNextTenant ? HouseStatus.RENTED : HouseStatus.REPAIRED);
        house.setUpdatedAt(Instant.now());
        houseRepository.save(house);
        cachedPageService.evictAll(PAGE_NS);
        houseEventProducer.publishTenantChanged(houseId, null);

        log.info("[House] Revoked access houseId={} tenantId={} status={}",
                houseId, tenantId, house.getStatus());
    }

    @Override
    @Transactional
    @CacheEvict(value = "user-house-access", allEntries = true)
    public void deactivateHouseForUser(UUID tenantId, UUID houseId, boolean keepUnavailable) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new NotFoundException("House not found: " + houseId));

        if (house.getUserRentalId() != null && !tenantId.equals(house.getUserRentalId())) {
            log.warn("[House] deactivateHouseForUser tenantId={} does not match userRentalId={} houseId={}",
                    tenantId, house.getUserRentalId(), houseId);
            return;
        }

        for (TenantGroup group : tenantGroupRepository.findAllByHouseIdAndIsActiveTrue(houseId)) {
            for (TenantMember m : tenantMemberRepository.findByTenantGroupId(group.getId())) {
                if (m.isActive()) {
                    m.setActive(false);
                    tenantMemberRepository.save(m);
                }
            }
            group.setActive(false);
            tenantGroupRepository.save(group);
            log.info("[House] Deactivated tenantGroupId={} houseId={}", group.getId(), houseId);
        }

        house.setUserRentalId(null);
        house.setTenantGroupId(null);
        house.setHandoverDate(null);
        boolean hasPendingNextTenant = house.getNextTenantId() != null && !tenantId.equals(house.getNextTenantId());
        if (tenantId.equals(house.getNextTenantId())) {
            house.setNextTenantId(null);
            house.setNextHandoverDate(null);
        }
        house.setStatus(keepUnavailable
                ? HouseStatus.REPAIRED
                : (hasPendingNextTenant ? HouseStatus.RENTED : HouseStatus.AVAILABLE));
        house.setUpdatedAt(Instant.now());
        houseRepository.save(house);
        cachedPageService.evictAll(PAGE_NS);
        houseEventProducer.publishTenantChanged(houseId, null);

        log.info("[House] Deactivated houseId={} tenantId={} status={}",
                houseId, tenantId, house.getStatus());

        if (house.getStatus() == HouseStatus.AVAILABLE) {
            subscriptionNotifier.notifyAvailable(house);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "user-house-access", allEntries = true)
    public void completeCheckoutAndHandover(
            UUID oldTenantId,
            UUID houseId,
            Instant effectiveAt,
            boolean demo) {
        House house = houseRepository.findByIdForUpdate(houseId)
                .orElseThrow(() -> new NotFoundException("House not found: " + houseId));

        if (house.getUserRentalId() != null
                && !oldTenantId.equals(house.getUserRentalId())) {
            log.warn("[House] Checkout tenant mismatch oldTenantId={} currentTenantId={} houseId={}",
                    oldTenantId, house.getUserRentalId(), houseId);
            return;
        }

        for (TenantGroup group : tenantGroupRepository.findAllByHouseIdAndIsActiveTrue(houseId)) {
            for (TenantMember member : tenantMemberRepository.findByTenantGroupId(group.getId())) {
                if (member.isActive()) {
                    member.setActive(false);
                    tenantMemberRepository.save(member);
                }
            }
            group.setActive(false);
            tenantGroupRepository.save(group);
        }

        UUID nextTenantId = house.getNextTenantId();
        Instant nextHandoverDate = house.getNextHandoverDate();
        house.setUserRentalId(null);
        house.setTenantGroupId(null);
        house.setHandoverDate(null);

        boolean handoverReached = nextTenantId != null
                && (nextHandoverDate == null || !nextHandoverDate.isAfter(effectiveAt));
        if (handoverReached) {
            InvoiceStatusDto payment = paymentGrpcClient.getInvoiceStatus(houseId, nextTenantId);
            if (payment.depositPaid() && payment.firstRentPaid()) {
                Instant accessDate = demo ? Instant.now() : nextHandoverDate;
                activateNow(house, nextTenantId, accessDate);
                log.info("[House] Checkout completed and next tenant activated houseId={} oldTenant={} nextTenant={} demo={}",
                        houseId, oldTenantId, nextTenantId, demo);
                return;
            }
            log.info("[House] Next tenant remains pending payment houseId={} nextTenant={} depositPaid={} firstRentPaid={}",
                    houseId, nextTenantId, payment.depositPaid(), payment.firstRentPaid());
        }

        boolean hasPendingNext = nextTenantId != null;
        house.setStatus(hasPendingNext ? HouseStatus.RENTED : HouseStatus.AVAILABLE);
        house.setUpdatedAt(Instant.now());
        houseRepository.save(house);
        cachedPageService.evictAll(PAGE_NS);
        houseEventProducer.publishTenantChanged(houseId, null);

        if (!hasPendingNext) {
            subscriptionNotifier.notifyAvailable(house);
        }
        log.info("[House] Checkout completed houseId={} oldTenant={} nextTenant={} handoverReached={} status={}",
                houseId, oldTenantId, nextTenantId, handoverReached, house.getStatus());
    }

    @Override
    @Transactional
    @CacheEvict(value = "user-house-access", allEntries = true)
    public HouseDto updateHouseStatus(UUID id, HouseStatus nextStatus, UUID actorId) {
        if (nextStatus == null) {
            throw new IllegalArgumentException("Status is required");
        }
        House house = houseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("House not found: " + id));

        HouseStatus current = house.getStatus();
        if (current == nextStatus) {
            return houseMapper.toDto(house);
        }
        if (current == HouseStatus.RENTED || nextStatus == HouseStatus.RENTED) {
            throw new IllegalStateException(
                    "RENTED status is driven by tenant lifecycle; manual transition is not allowed.");
        }

        house.setStatus(nextStatus);
        house.setUpdatedAt(Instant.now());
        House saved = houseRepository.save(house);
        cachedPageService.evictAll(PAGE_NS);

        log.info("[House] Status updated houseId={} {} -> {} actor={}",
                id, current, nextStatus, actorId);

        if (nextStatus == HouseStatus.AVAILABLE) {
            subscriptionNotifier.notifyAvailable(saved);
        }
        return houseMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void subscribeToAvailability(UUID houseId, String keycloakId) {
        UserResponse user = userClientsGrpc.getUserIdAndRoleByKeyCloakId(keycloakId);
        UUID userId = UUID.fromString(user.getId());
        if (subscriptionRepository.existsByUserIdAndHouseId(userId, houseId)) {
            return;
        }
        if (!houseRepository.existsById(houseId)) {
            throw new NotFoundException("House not found: " + houseId);
        }
        subscriptionRepository.save(HouseSubscription.builder()
                .userId(userId)
                .houseId(houseId)
                .userEmail(user.getEmail())
                .build());
        log.info("[Subscription] userId={} subscribed houseId={}", userId, houseId);
    }

    @Override
    @Transactional
    public void unsubscribeFromAvailability(UUID houseId, String keycloakId) {
        UserResponse user = userClientsGrpc.getUserIdAndRoleByKeyCloakId(keycloakId);
        UUID userId = UUID.fromString(user.getId());
        int deleted = subscriptionRepository.deleteByUserIdAndHouseId(userId, houseId);
        if (deleted > 0) {
            log.info("[Subscription] userId={} unsubscribed houseId={}", userId, houseId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSubscribed(UUID houseId, String keycloakId) {
        UserResponse user = userClientsGrpc.getUserIdAndRoleByKeyCloakId(keycloakId);
        UUID userId = UUID.fromString(user.getId());
        return subscriptionRepository.existsByUserIdAndHouseId(userId, houseId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "user-house-access", allEntries = true)
    public void setTenantAccessRestriction(UUID tenantId, UUID houseId, boolean restricted) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new NotFoundException("House not found: " + houseId));

        if (!tenantId.equals(house.getUserRentalId())) {
            log.warn("[House] setTenantAccessRestriction tenantId={} is not a tenant of houseId={}",
                    tenantId, houseId);
            return;
        }

        house.setPaymentRestricted(restricted);
        house.setUpdatedAt(Instant.now());
        houseRepository.save(house);
        cachedPageService.evictAll(PAGE_NS);

        log.info("[House] PaymentRestricted={} houseId={} tenantId={}",
                restricted, houseId, tenantId);
    }

    private void createPendingTenantGroup(UUID userId, UUID houseId) {
        TenantGroup group = tenantGroupRepository.findAllByHouseId(houseId).stream()
                .filter(g -> !g.isActive())
                .findFirst()
                .orElseGet(() -> tenantGroupRepository.save(TenantGroup.builder()
                        .houseId(houseId)
                        .isActive(false)
                        .build()));

        TenantMemberId memberId = new TenantMemberId();
        memberId.setTenantId(group.getId());
        memberId.setUserId(userId);

        if (!tenantMemberRepository.existsById(memberId)) {
            tenantMemberRepository.save(TenantMember.builder()
                    .id(memberId)
                    .tenantGroup(group)
                    .isOwner(true)
                    .isActive(false)
                    .build());
        }

        log.info("[House] Pending TenantGroup created for userId={} houseId={}", userId, houseId);
    }

    private void activateNow(House house, UUID userId, Instant handoverDate) {
        deactivateOtherActiveHousesOfUser(userId, house.getId());

        if (house.getTenantGroupId() != null) {
            tenantGroupRepository.findById(house.getTenantGroupId()).ifPresent(g -> {
                g.setActive(false);
                tenantGroupRepository.save(g);
            });
        }

        TenantGroup group = resolveActiveTenantGroupForActivation(house.getId(), userId);

        TenantMemberId memberId = new TenantMemberId();
        memberId.setTenantId(group.getId());
        memberId.setUserId(userId);

        if (!tenantMemberRepository.existsById(memberId)) {
            tenantMemberRepository.save(TenantMember.builder()
                    .id(memberId)
                    .tenantGroup(group)
                    .isOwner(true)
                    .isActive(true)
                    .build());
        }

        house.setUserRentalId(userId);
        house.setTenantGroupId(group.getId());
        house.setHandoverDate(handoverDate);
        house.setNextTenantId(null);
        house.setNextHandoverDate(null);
        house.setStatus(HouseStatus.RENTED);
        houseRepository.save(house);
        cachedPageService.evictAll(PAGE_NS);
        houseEventProducer.publishTenantChanged(house.getId(), userId);

        log.info("[House] Activated houseId={} ownerId={}", house.getId(), userId);
    }

    private TenantGroup resolveActiveTenantGroupForActivation(UUID houseId, UUID userId) {
        List<TenantGroup> activeGroups = tenantGroupRepository.findAllByHouseIdAndIsActiveTrue(houseId);

        TenantGroup selected = activeGroups.stream()
                .filter(g -> tenantMemberRepository.existsByTenantGroupIdAndUserId(g.getId(), userId))
                .findFirst()
                .orElse(null);

        for (TenantGroup group : activeGroups) {
            if (selected == null || !group.getId().equals(selected.getId())) {
                group.setActive(false);
                tenantGroupRepository.save(group);
                log.warn("[House] Deactivated duplicate active tenantGroupId={} houseId={}",
                        group.getId(), houseId);
            }
        }

        if (selected != null) {
            return selected;
        }

        return tenantGroupRepository.save(TenantGroup.builder()
                .houseId(houseId)
                .isActive(true)
                .build());
    }

    private PageResponse<HouseDto> loadPage(PageRequest request) {
        HouseStatus statusFilter = request.<String>filterValue("status")
                .map(s -> {
                    try {
                        return HouseStatus.valueOf(s.toUpperCase().trim());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .orElse(null);

        String statusesRaw = request.<String>filterValue("statuses").orElse(null);

        String houseIdRaw = request.<String>filterValue("houseId").orElse(null);
        UUID houseIdFilter = houseIdRaw != null ? UUID.fromString(houseIdRaw) : null;

        List<HouseStatus> statusFilters = statusesRaw == null || statusesRaw.isBlank()
                ? List.of()
                : Arrays.stream(statusesRaw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(String::toUpperCase)
                .map(value -> {
                    try {
                        return HouseStatus.valueOf(value);
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        Specification<House> spec = (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (request.hasKeyword()) {
                String keyword = "%" + request.keyword().trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("address")), keyword)
                ));
            }

            if (statusFilter != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), statusFilter));
            }

            if (!statusFilters.isEmpty()) {
                predicates.add(root.get("status").in(statusFilters));
            }

            if (houseIdFilter != null) {
                predicates.add(criteriaBuilder.equal(root.get("id"), houseIdFilter));
            }

            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };

        var pageable = SpringPageConverter.toPageable(request);

        Page<House> page = houseRepository.findAll(spec, pageable);

        List<House> houses = page.getContent();

        List<HouseDto> housedtos = houses.stream()
                .map(house -> {
                    HouseDto dto = houseMapper.toDto(house);

                    List<HouseImageDto> images = getHouseImages(house.getId());

                        return new HouseDto(
                                dto.id(),
                                dto.userRentalId(),
                                dto.regionId(),
                                dto.name(),
                                dto.address(),
                                dto.ward(),
                                dto.commune(),
                                dto.city(),
                                dto.numberOfFloors(),
                                dto.areaM2(),
                                dto.structure(),
                                dto.landCertNumber(),
                                dto.landCertIssueDate(),
                                dto.landCertIssuer(),
                                dto.paymentRestricted(),
                                dto.description(),
                                dto.status(),
                                dto.functionalAreas(),
                                images,
                                dto.nameTranslations(),
                                dto.addressTranslations(),
                                dto.wardTranslations(),
                                dto.communeTranslations(),
                                dto.cityTranslations(),
                                dto.descriptionTranslations()
                        );
                    })
                    .toList();
            return PageResponse.of(
                    housedtos,
                    page.hasNext(),
                    page.getTotalElements(),
                    page.getTotalPages(),
                    page.getNumber(),
                    page.getSize()
            );
        }
    private Duration contractEndBuffer() {
        return Duration.ofDays(1);
    }

    private Map<UUID, Integer> loadAssetCountByArea(UUID houseId) {
        String bearerToken = currentBearerToken();
        if (bearerToken == null || bearerToken.isBlank()) {
            return Map.of();
        }

        try {
            return assetRestClient.getAssetCountByHouseId(houseId, bearerToken).stream()
                    .filter(asset -> asset.functionAreaId() != null)
                    .collect(Collectors.toMap(
                            AssetRestClient.AreaAssetCountDto::functionAreaId,
                            asset -> Math.toIntExact(asset.assetCount())
                    ));
        } catch (Exception ex) {
            log.warn("[House] loadAssetCountByArea failed houseId={}: {}", houseId, ex.getMessage());
            return Map.of();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<HouseHistoryItemDto> getHouseHistory(UUID houseId) {
        if (!houseRepository.existsById(houseId)) {
            throw new RuntimeException("House not found");
        }

        String bearerToken = currentBearerToken();
        if (bearerToken == null || bearerToken.isBlank()) {
            return List.of();
        }

        List<HouseHistoryItemDto> items = new ArrayList<>();

        try {
            items.addAll(houseHistoryRestClient.getMaintenanceJobsByHouseId(houseId, bearerToken)
                    .stream()
                    .map(this::toHistoryItem)
                    .toList());
        } catch (Exception ex) {
            log.warn("[House] getHouseHistory maintenance failed houseId={}: {}", houseId, ex.getMessage());
        }

        try {
            items.addAll(houseHistoryRestClient.getInspectionsByHouseId(houseId, bearerToken)
                    .stream()
                    .map(this::toHistoryItem)
                    .toList());
        } catch (Exception ex) {
            log.warn("[House] getHouseHistory inspections failed houseId={}: {}", houseId, ex.getMessage());
        }

        try {
            items.addAll(houseHistoryRestClient.getIssueTicketsByHouseId(houseId, bearerToken)
                    .stream()
                    .map(this::toHistoryItem)
                    .toList());
        } catch (Exception ex) {
            log.warn("[House] getHouseHistory issues failed houseId={}: {}", houseId, ex.getMessage());
        }

        return items.stream()
                .sorted(Comparator
                        .comparing(HouseHistoryItemDto::happenedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(HouseHistoryItemDto::createdAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .toList();
    }

    private String currentBearerToken() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        if (principal instanceof Jwt jwt) {
            return jwt.getTokenValue();
        }
        return null;
    }

    private HouseHistoryItemDto toHistoryItem(HouseHistoryRestClient.MaintenanceJobItemDto job) {
        Instant happenedAt = job.periodStartDate() != null
                ? job.periodStartDate().atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant()
                : null;
        return new HouseHistoryItemDto(
                job.id(),
                "MAINTENANCE",
                "PERIODIC_MAINTENANCE",
                job.status(),
                "Periodic maintenance",
                null,
                null,
                null,
                job.assignedStaffId(),
                job.staffName(),
                job.staffPhone(),
                happenedAt,
                happenedAt
        );
    }

    private HouseHistoryItemDto toHistoryItem(HouseHistoryRestClient.InspectionItemDto inspection) {
        String title = switch (String.valueOf(inspection.type())) {
            case "CHECK_IN" -> "Check-in inspection";
            case "CHECK_OUT" -> "Check-out inspection";
            default -> "House inspection";
        };
        return new HouseHistoryItemDto(
                inspection.id(),
                "INSPECTION",
                inspection.type(),
                inspection.status(),
                title,
                inspection.note(),
                null,
                inspection.slotId(),
                inspection.assignedStaffId(),
                inspection.staffName(),
                inspection.staffPhone(),
                inspection.createdAt(),
                inspection.updatedAt() != null ? inspection.updatedAt() : inspection.createdAt()
        );
    }

    @Override
    @Transactional
    public HouseDto updateHouse(UUID id, CreateHouseRequest req) {
        House house = houseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("House not found: " + id));
        if (req.regionId() != null && (house.getRegion() == null || !req.regionId().equals(house.getRegion().getId()))) {
            Region region = regionRepository.findById(req.regionId())
                    .orElseThrow(() -> new RuntimeException("Region not found"));
            house.setRegion(region);
        }
        if (req.name() != null)        house.setName(req.name());
        if (req.address() != null)     house.setAddress(req.address());
        if (req.ward() != null)        house.setWard(req.ward());
        if (req.commune() != null)     house.setCommune(req.commune());
        if (req.city() != null)        house.setCity(req.city());
        if (req.description() != null) house.setDescription(req.description());
        if (req.numberOfFloors() != null) house.setNumberOfFloors(req.numberOfFloors());
        if (req.areaM2() != null)      house.setAreaM2(req.areaM2());
        if (req.structure() != null)   house.setStructure(req.structure());
        if (req.landCertNumber() != null)    house.setLandCertNumber(req.landCertNumber());
        if (req.landCertIssueDate() != null) house.setLandCertIssueDate(req.landCertIssueDate());
        if (req.landCertIssuer() != null)    house.setLandCertIssuer(req.landCertIssuer());
        if (req.nameTranslations() != null)        house.setNameTranslations(translationAutoFillService.complete(req.name(), req.nameTranslations()));
        if (req.addressTranslations() != null)     house.setAddressTranslations(translationAutoFillService.complete(req.address(), req.addressTranslations()));
        if (req.wardTranslations() != null)        house.setWardTranslations(translationAutoFillService.complete(req.ward(), req.wardTranslations()));
        if (req.communeTranslations() != null)     house.setCommuneTranslations(translationAutoFillService.complete(req.commune(), req.communeTranslations()));
        if (req.cityTranslations() != null)        house.setCityTranslations(translationAutoFillService.complete(req.city(), req.cityTranslations()));
        if (req.descriptionTranslations() != null) house.setDescriptionTranslations(translationAutoFillService.complete(req.description(), req.descriptionTranslations()));
        house.setUpdatedAt(Instant.now());
        House saved = houseRepository.save(house);
        cachedPageService.evictAll(PAGE_NS);
        return houseMapper.toDto(saved);
    }

    private HouseHistoryItemDto toHistoryItem(HouseHistoryRestClient.IssueTicketItemDto issue) {
        Instant happenedAt = issue.startTime() != null
                ? issue.startTime().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant()
                : issue.createdAt();
        return new HouseHistoryItemDto(
                issue.id(),
                "ISSUE",
                issue.type(),
                issue.status(),
                issue.title(),
                issue.description(),
                issue.assetId(),
                issue.slotId(),
                issue.assignedStaffId(),
                issue.staffName(),
                issue.staffPhone(),
                issue.createdAt(),
                happenedAt
        );
    }

    private void deactivateOtherActiveHousesOfUser(UUID userId, UUID newHouseId) {
        List<House> currentHouses = houseRepository.findByUserRentalId(userId);

        for (House oldHouse : currentHouses) {
            if (!oldHouse.getId().equals(newHouseId)) {
                for (TenantGroup group : tenantGroupRepository.findAllByHouseIdAndIsActiveTrue(oldHouse.getId())) {
                    for (TenantMember m : tenantMemberRepository.findByTenantGroupId(group.getId())) {
                        if (m.isActive()) {
                            m.setActive(false);
                            tenantMemberRepository.save(m);
                        }
                    }
                    group.setActive(false);
                    tenantGroupRepository.save(group);
                }

                oldHouse.setUserRentalId(null);
                oldHouse.setTenantGroupId(null);
                oldHouse.setHandoverDate(null);

                if (userId.equals(oldHouse.getNextTenantId())) {
                    oldHouse.setNextTenantId(null);
                    oldHouse.setNextHandoverDate(null);
                }

                oldHouse.setUpdatedAt(Instant.now());
                houseRepository.save(oldHouse);

                log.info("[House] Auto-deactivated old houseId={} for userId={} before activating newHouseId={}",
                        oldHouse.getId(), userId, newHouseId);
            }
        }
    }
}
