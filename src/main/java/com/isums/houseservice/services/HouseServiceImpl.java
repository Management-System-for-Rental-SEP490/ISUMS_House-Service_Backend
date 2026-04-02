package com.isums.houseservice.services;

import com.isums.houseservice.domains.dtos.*;
import com.isums.houseservice.domains.emuns.AccessStatus;
import com.isums.houseservice.domains.emuns.HouseMemberRole;
import com.isums.houseservice.domains.entities.*;
import com.isums.houseservice.exceptions.NotFoundException;
import com.isums.houseservice.infrastructures.abstracts.HouseService;
import com.isums.houseservice.domains.emuns.HouseStatus;
import com.isums.houseservice.infrastructures.grpcs.PaymentGrpcClient;
import com.isums.houseservice.infrastructures.grpcs.UserClientsGrpc;
import com.isums.houseservice.infrastructures.kafkas.HouseEventProducer;
import com.isums.houseservice.infrastructures.mappers.HouseMapper;
import com.isums.houseservice.infrastructures.repositories.*;
import com.isums.userservice.grpc.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HouseServiceImpl implements HouseService {

    private final HouseRepository houseRepository;
    private final HouseMapper houseMapper;
    private final RegionRepository regionRepository;
    private final HouseEventProducer houseEventProducer;
    private final S3ServiceImpl s3;
    private final HouseImageRepository houseImageRepository;
    private final UserClientsGrpc userClientsGrpc;
    private final PaymentGrpcClient paymentGrpcClient;
    private final TenantGroupRepository tenantGroupRepository;
    private final TenantMemberRepository tenantMemberRepository;

    private static final DateTimeFormatter DMY = DateTimeFormatter
            .ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    @Override
    public HouseDto CreateHouse(CreateHouseRequest req) {
        Region region = regionRepository.findById(req.regionId())
                .orElseThrow(() -> new RuntimeException("Region not found"));

        House house = House.builder()
                .name(req.name())
                .address(req.address())
                .region(region)
                .commune(req.commune())
                .city(req.city())
                .description(req.description())
                .status(HouseStatus.AVAILABLE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        House created = houseRepository.save(house);
        houseEventProducer.publishHouseCreated(created.getId());
        return houseMapper.toDto(created);
    }

    @Override
    @Cacheable(value = "allHouses")
    @Transactional(readOnly = true)
    public List<HouseDto> GetAllHouses() {
        try {

            List<House> houses = houseRepository.findAll();
            return houseMapper.toDtos(houses);
        } catch (Exception ex) {
            throw new RuntimeException("Error to get all houses");
        }
    }

    @Override
    @Cacheable(value = "houseInformation", key = "#id")
    @Transactional(readOnly = true)
    public HouseDto getHouseById(UUID id) {
        try {
            House house = houseRepository.findWithFunctionalAreasById(id)
                    .orElseThrow(() -> new RuntimeException("House not found"));

            return houseMapper.toDto(house);
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
    }

    @Override
    @Cacheable(value = "houseImages", key = "#houseId")
    public List<HouseImageDto> getHouseImages(UUID houseId) {
        List<HouseImage> images = houseImageRepository.findByHouseId(houseId);

        List<HouseImageDto> imagesDto = new ArrayList<>();
        images.forEach(image -> {
            String url = s3.getImageUrl(image.getKey());
            imagesDto.add(new HouseImageDto(image.getId(), url, image.getCreatedAt()));
        });

        return imagesDto;
    }

    @Override
    @CacheEvict(value = "houseImages", key = "#houseId", allEntries = true)
    public void deleteHouseImage(UUID houseId, UUID imageId) {
        HouseImage image = houseImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("House image not found"));
        s3.delete(image.getKey());
        houseImageRepository.delete(image);
    }

    @Override
    @Transactional
    public void activeHouseForUser(UUID userId, UUID houseId, Instant handoverDate) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new NotFoundException("House not found: " + houseId));

        boolean houseCurrentlyOccupied = house.getUserRentalId() != null
                && house.getStatus() == HouseStatus.RENTED
                && house.getHandoverDate() != null
                && Instant.now().isBefore(house.getHandoverDate().plus(contractEndBuffer()));

        if (houseCurrentlyOccupied && handoverDate != null && handoverDate.isAfter(Instant.now())) {
            house.setNextTenantId(userId);
            house.setNextHandoverDate(handoverDate);
            houseRepository.save(house);

            createPendingTenantGroup(userId, houseId);

            log.info("[House] Pending next tenant userId={} houseId={} handoverDate={}",
                    userId, houseId, handoverDate);
        } else {
            activateNow(house, userId, handoverDate);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<HouseAccessStatus> getMyHouseAccess(UUID userId) {
        List<House> houses = houseRepository.findByTenantGroupMemberUserId(userId);

        return houses.stream().map(house -> {
            Instant now = Instant.now();

            HouseMemberRole role = userId.equals(house.getUserRentalId())
                    ? HouseMemberRole.OWNER
                    : HouseMemberRole.MEMBER;

            InvoiceStatusDto invoiceStatus = paymentGrpcClient.getInvoiceStatus(house.getId(), userId);

            AccessStatus status;
            String reason = null;

            if (!invoiceStatus.depositPaid()) {
                status = AccessStatus.PENDING_DEPOSIT;
                reason = "ACCESS_PENDING_DEPOSIT";
            } else if (house.getHandoverDate() != null && now.isBefore(house.getHandoverDate())) {
                status = AccessStatus.PENDING_HANDOVER;
                reason = "ACCESS_PENDING_HANDOVER";
            } else if (!invoiceStatus.firstRentPaid()) {
                status = AccessStatus.PENDING_FIRST_RENT;
                reason = "ACCESS_PENDING_FIRST_RENT";
            } else {
                status = AccessStatus.ACCESSIBLE;
            }

            return new HouseAccessStatus(
                    house.getId(),
                    house.getName(),
                    house.getAddress(),
                    house.getHandoverDate(),
                    status,
                    reason,
                    invoiceStatus.pendingInvoiceId() != null,
                    invoiceStatus.pendingInvoiceId(),
                    role
            );
        }).toList();
    }

    private void createPendingTenantGroup(UUID userId, UUID houseId) {
        TenantGroup group = tenantGroupRepository.findByHouseId(houseId)
                .filter(g -> !g.isActive())
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
        if (house.getTenantGroupId() != null) {
            tenantGroupRepository.findById(house.getTenantGroupId()).ifPresent(g -> {
                g.setActive(false);
                tenantGroupRepository.save(g);
            });
        }

        TenantGroup group = tenantGroupRepository.findByHouseIdAndIsActiveTrue(house.getId())
                .filter(g -> tenantMemberRepository.existsByTenantGroupIdAndUserId(g.getId(), userId))
                .orElseGet(() -> tenantGroupRepository.save(TenantGroup.builder()
                        .houseId(house.getId())
                        .isActive(true)
                        .build()));

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

        log.info("[House] Activated houseId={} ownerId={}", house.getId(), userId);
    }

    private Duration contractEndBuffer() {
        return Duration.ofDays(1);
    }
}
