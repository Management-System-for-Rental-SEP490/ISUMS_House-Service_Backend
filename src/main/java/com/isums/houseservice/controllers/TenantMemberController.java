package com.isums.houseservice.controllers;

import com.isums.houseservice.domains.dtos.ApiResponse;
import com.isums.houseservice.domains.dtos.ApiResponses;
import com.isums.houseservice.domains.dtos.TenantMemberDto;
import com.isums.houseservice.infrastructures.abstracts.TenantMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/houses/{houseId}/members")
@RequiredArgsConstructor
public class TenantMemberController {

    private final TenantMemberService tenantMemberService;

    @GetMapping
    @PreAuthorize("hasRole('TENANT')")
    public ApiResponse<List<TenantMemberDto>> getMembers(@PathVariable UUID houseId) {
        return ApiResponses.ok(tenantMemberService.getMembers(houseId), "Members retrieved successfully");
    }

    @PostMapping("/{userId}")
    @PreAuthorize("hasRole('TENANT')")
    public ApiResponse<Void> addMember(
            @PathVariable UUID houseId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getSubject());
        tenantMemberService.addMember(houseId, requesterId, userId);
        return ApiResponses.ok(null, "Member added successfully");
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('TENANT')")
    public ApiResponse<Void> removeMember(
            @PathVariable UUID houseId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getSubject());
        tenantMemberService.removeMember(houseId, requesterId, userId);
        return ApiResponses.ok(null, "Member removed successfully");
    }
}