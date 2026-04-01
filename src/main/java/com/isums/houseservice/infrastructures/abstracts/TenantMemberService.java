package com.isums.houseservice.infrastructures.abstracts;

import com.isums.houseservice.domains.dtos.TenantMemberDto;

import java.util.List;
import java.util.UUID;

public interface TenantMemberService {
    void addMember(UUID houseId, UUID requesterId, UUID newUserId);
    void removeMember(UUID houseId, UUID requesterId, UUID targetUserId);
    List<TenantMemberDto> getMembers(UUID houseId);
}