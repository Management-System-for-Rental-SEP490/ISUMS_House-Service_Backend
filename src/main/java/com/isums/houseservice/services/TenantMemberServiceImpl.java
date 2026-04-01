package com.isums.houseservice.services;

import com.isums.houseservice.domains.dtos.TenantMemberDto;
import com.isums.houseservice.domains.emuns.HouseMemberRole;
import com.isums.houseservice.domains.entities.*;
import com.isums.houseservice.exceptions.HouseErrorCode;
import com.isums.houseservice.exceptions.HouseException;
import com.isums.houseservice.infrastructures.abstracts.TenantMemberService;
import com.isums.houseservice.infrastructures.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantMemberServiceImpl implements TenantMemberService {

    private final HouseRepository houseRepository;
    private final TenantGroupRepository tenantGroupRepository;
    private final TenantMemberRepository tenantMemberRepository;

    @Override
    @Transactional
    public void addMember(UUID houseId, UUID requesterId, UUID newUserId) {
        House house = getHouseOrThrow(houseId);
        validateIsOwner(house, requesterId);

        TenantGroup group = getTenantGroupOrThrow(houseId);

        TenantMemberId memberId = buildMemberId(group.getId(), newUserId);
        if (tenantMemberRepository.existsById(memberId)) {
            throw new HouseException(HouseErrorCode.MEMBER_ALREADY_EXISTS);
        }

        tenantMemberRepository.save(TenantMember.builder()
                .id(memberId)
                .tenantGroup(group)
                .isOwner(false)
                .isActive(true)
                .build());

        log.info("[TenantMember] Added userId={} to houseId={} by ownerId={}",
                newUserId, houseId, requesterId);
    }

    @Override
    @Transactional
    public void removeMember(UUID houseId, UUID requesterId, UUID targetUserId) {
        House house = getHouseOrThrow(houseId);
        validateIsOwner(house, requesterId);

        if (targetUserId.equals(requesterId)) {
            throw new HouseException(HouseErrorCode.CANNOT_REMOVE_SELF);
        }

        TenantGroup group = getTenantGroupOrThrow(houseId);
        TenantMemberId memberId = buildMemberId(group.getId(), targetUserId);

        TenantMember member = tenantMemberRepository.findById(memberId)
                .orElseThrow(() -> new HouseException(HouseErrorCode.MEMBER_NOT_FOUND));

        if (member.isOwner()) {
            throw new HouseException(HouseErrorCode.CANNOT_REMOVE_OWNER);
        }

        tenantMemberRepository.delete(member);

        log.info("[TenantMember] Removed userId={} from houseId={} by ownerId={}",
                targetUserId, houseId, requesterId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantMemberDto> getMembers(UUID houseId) {
        TenantGroup group = getTenantGroupOrThrow(houseId);

        return tenantMemberRepository.findByTenantGroupId(group.getId())
                .stream()
                .map(m -> new TenantMemberDto(
                        m.getUserId(),
                        m.isOwner() ? HouseMemberRole.OWNER : HouseMemberRole.MEMBER,
                        m.isActive(),
                        m.getCreatedAt()))
                .toList();
    }

    private void validateIsOwner(House house, UUID requesterId) {
        if (!requesterId.equals(house.getUserRentalId())) {
            throw new HouseException(HouseErrorCode.NOT_HOUSE_OWNER);
        }
    }

    private House getHouseOrThrow(UUID houseId) {
        return houseRepository.findById(houseId)
                .orElseThrow(() -> new HouseException(HouseErrorCode.HOUSE_NOT_FOUND));
    }

    private TenantGroup getTenantGroupOrThrow(UUID houseId) {
        return tenantGroupRepository.findByHouseId(houseId)
                .orElseThrow(() -> new HouseException(HouseErrorCode.TENANT_GROUP_NOT_FOUND));
    }

    private TenantMemberId buildMemberId(UUID tenantGroupId, UUID userId) {
        TenantMemberId id = new TenantMemberId();
        id.setTenantId(tenantGroupId);
        id.setUserId(userId);
        return id;
    }
}