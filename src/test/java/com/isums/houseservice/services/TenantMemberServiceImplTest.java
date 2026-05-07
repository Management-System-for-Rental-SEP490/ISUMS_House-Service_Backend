package com.isums.houseservice.services;

import com.isums.houseservice.domains.dtos.TenantMemberDto;
import com.isums.houseservice.domains.emuns.HouseMemberRole;
import com.isums.houseservice.domains.entities.House;
import com.isums.houseservice.domains.entities.TenantGroup;
import com.isums.houseservice.domains.entities.TenantMember;
import com.isums.houseservice.domains.entities.TenantMemberId;
import com.isums.houseservice.exceptions.HouseErrorCode;
import com.isums.houseservice.exceptions.HouseException;
import com.isums.houseservice.infrastructures.repositories.HouseRepository;
import com.isums.houseservice.infrastructures.repositories.TenantGroupRepository;
import com.isums.houseservice.infrastructures.repositories.TenantMemberRepository;
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
@DisplayName("TenantMemberServiceImpl")
class TenantMemberServiceImplTest {

    @Mock private HouseRepository houseRepository;
    @Mock private TenantGroupRepository tenantGroupRepository;
    @Mock private TenantMemberRepository tenantMemberRepository;

    @InjectMocks private TenantMemberServiceImpl service;

    private UUID houseId;
    private UUID ownerId;
    private UUID newUserId;
    private UUID tenantGroupId;
    private House house;
    private TenantGroup group;

    @BeforeEach
    void setUp() {
        houseId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        newUserId = UUID.randomUUID();
        tenantGroupId = UUID.randomUUID();
        house = House.builder().id(houseId).userRentalId(ownerId).build();
        group = TenantGroup.builder().id(tenantGroupId).houseId(houseId).isActive(true).build();
    }

    @Nested
    @DisplayName("addMember")
    class AddMember {

        @Test
        @DisplayName("saves new member when requester is owner and member not yet present")
        void happyPath() {
            when(houseRepository.findById(houseId)).thenReturn(Optional.of(house));
            when(tenantGroupRepository.findByHouseId(houseId)).thenReturn(Optional.of(group));
            when(tenantMemberRepository.existsById(any(TenantMemberId.class))).thenReturn(false);

            service.addMember(houseId, ownerId, newUserId);

            ArgumentCaptor<TenantMember> cap = ArgumentCaptor.forClass(TenantMember.class);
            verify(tenantMemberRepository).save(cap.capture());
            TenantMember saved = cap.getValue();
            assertThat(saved.getUserId()).isEqualTo(newUserId);
            assertThat(saved.getTenantGroupId()).isEqualTo(tenantGroupId);
            assertThat(saved.isOwner()).isFalse();
            assertThat(saved.isActive()).isTrue();
        }

        @Test
        @DisplayName("throws HOUSE_NOT_FOUND when house missing")
        void houseMissing() {
            when(houseRepository.findById(houseId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addMember(houseId, ownerId, newUserId))
                    .isInstanceOf(HouseException.class)
                    .satisfies(ex -> assertThat(((HouseException) ex).getCode())
                            .isEqualTo(HouseErrorCode.HOUSE_NOT_FOUND));
        }

        @Test
        @DisplayName("throws NOT_HOUSE_OWNER when requester is not owner")
        void notOwner() {
            UUID randomRequester = UUID.randomUUID();
            when(houseRepository.findById(houseId)).thenReturn(Optional.of(house));

            assertThatThrownBy(() -> service.addMember(houseId, randomRequester, newUserId))
                    .isInstanceOf(HouseException.class)
                    .satisfies(ex -> assertThat(((HouseException) ex).getCode())
                            .isEqualTo(HouseErrorCode.NOT_HOUSE_OWNER));
        }

        @Test
        @DisplayName("throws TENANT_GROUP_NOT_FOUND when group missing")
        void groupMissing() {
            when(houseRepository.findById(houseId)).thenReturn(Optional.of(house));
            when(tenantGroupRepository.findByHouseId(houseId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addMember(houseId, ownerId, newUserId))
                    .isInstanceOf(HouseException.class)
                    .satisfies(ex -> assertThat(((HouseException) ex).getCode())
                            .isEqualTo(HouseErrorCode.TENANT_GROUP_NOT_FOUND));
        }

        @Test
        @DisplayName("throws MEMBER_ALREADY_EXISTS when member already present")
        void alreadyExists() {
            when(houseRepository.findById(houseId)).thenReturn(Optional.of(house));
            when(tenantGroupRepository.findByHouseId(houseId)).thenReturn(Optional.of(group));
            when(tenantMemberRepository.existsById(any(TenantMemberId.class))).thenReturn(true);

            assertThatThrownBy(() -> service.addMember(houseId, ownerId, newUserId))
                    .isInstanceOf(HouseException.class)
                    .satisfies(ex -> assertThat(((HouseException) ex).getCode())
                            .isEqualTo(HouseErrorCode.MEMBER_ALREADY_EXISTS));
            verify(tenantMemberRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("removeMember")
    class RemoveMember {

        private TenantMember nonOwnerMember() {
            TenantMemberId id = new TenantMemberId();
            id.setTenantId(tenantGroupId);
            id.setUserId(newUserId);
            return TenantMember.builder().id(id).tenantGroup(group).isOwner(false).isActive(true).build();
        }

        @Test
        @DisplayName("deletes non-owner member when requester is owner")
        void happyPath() {
            TenantMember member = nonOwnerMember();

            when(houseRepository.findById(houseId)).thenReturn(Optional.of(house));
            when(tenantGroupRepository.findByHouseId(houseId)).thenReturn(Optional.of(group));
            when(tenantMemberRepository.findById(any(TenantMemberId.class)))
                    .thenReturn(Optional.of(member));

            service.removeMember(houseId, ownerId, newUserId);

            verify(tenantMemberRepository).delete(member);
        }

        @Test
        @DisplayName("throws CANNOT_REMOVE_SELF when requester equals target")
        void cannotRemoveSelf() {
            when(houseRepository.findById(houseId)).thenReturn(Optional.of(house));

            assertThatThrownBy(() -> service.removeMember(houseId, ownerId, ownerId))
                    .isInstanceOf(HouseException.class)
                    .satisfies(ex -> assertThat(((HouseException) ex).getCode())
                            .isEqualTo(HouseErrorCode.CANNOT_REMOVE_SELF));
        }

        @Test
        @DisplayName("throws MEMBER_NOT_FOUND when member absent")
        void memberMissing() {
            when(houseRepository.findById(houseId)).thenReturn(Optional.of(house));
            when(tenantGroupRepository.findByHouseId(houseId)).thenReturn(Optional.of(group));
            when(tenantMemberRepository.findById(any(TenantMemberId.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeMember(houseId, ownerId, newUserId))
                    .isInstanceOf(HouseException.class)
                    .satisfies(ex -> assertThat(((HouseException) ex).getCode())
                            .isEqualTo(HouseErrorCode.MEMBER_NOT_FOUND));
        }

        @Test
        @DisplayName("throws CANNOT_REMOVE_OWNER when target member is owner")
        void cannotRemoveOwner() {
            TenantMemberId id = new TenantMemberId();
            id.setTenantId(tenantGroupId);
            id.setUserId(newUserId);
            TenantMember owner = TenantMember.builder()
                    .id(id).tenantGroup(group).isOwner(true).isActive(true).build();

            when(houseRepository.findById(houseId)).thenReturn(Optional.of(house));
            when(tenantGroupRepository.findByHouseId(houseId)).thenReturn(Optional.of(group));
            when(tenantMemberRepository.findById(any(TenantMemberId.class))).thenReturn(Optional.of(owner));

            assertThatThrownBy(() -> service.removeMember(houseId, ownerId, newUserId))
                    .isInstanceOf(HouseException.class)
                    .satisfies(ex -> assertThat(((HouseException) ex).getCode())
                            .isEqualTo(HouseErrorCode.CANNOT_REMOVE_OWNER));
            verify(tenantMemberRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws NOT_HOUSE_OWNER when requester is not owner")
        void notOwner() {
            UUID randomRequester = UUID.randomUUID();
            when(houseRepository.findById(houseId)).thenReturn(Optional.of(house));

            assertThatThrownBy(() -> service.removeMember(houseId, randomRequester, newUserId))
                    .isInstanceOf(HouseException.class)
                    .satisfies(ex -> assertThat(((HouseException) ex).getCode())
                            .isEqualTo(HouseErrorCode.NOT_HOUSE_OWNER));
        }
    }

    @Nested
    @DisplayName("getMembers")
    class GetMembers {

        @Test
        @DisplayName("maps owner+member roles correctly")
        void rolesMapped() {
            TenantMemberId oid = new TenantMemberId();
            oid.setTenantId(tenantGroupId);
            oid.setUserId(ownerId);
            TenantMember owner = TenantMember.builder()
                    .id(oid).tenantGroup(group).isOwner(true).isActive(true)
                    .createdAt(Instant.now()).build();

            TenantMemberId mid = new TenantMemberId();
            mid.setTenantId(tenantGroupId);
            mid.setUserId(newUserId);
            TenantMember member = TenantMember.builder()
                    .id(mid).tenantGroup(group).isOwner(false).isActive(true)
                    .createdAt(Instant.now()).build();

            when(tenantGroupRepository.findByHouseId(houseId)).thenReturn(Optional.of(group));
            when(tenantMemberRepository.findByTenantGroupId(tenantGroupId))
                    .thenReturn(List.of(owner, member));

            List<TenantMemberDto> res = service.getMembers(houseId);

            assertThat(res).hasSize(2);
            assertThat(res).anySatisfy(dto -> {
                assertThat(dto.userId()).isEqualTo(ownerId);
                assertThat(dto.role()).isEqualTo(HouseMemberRole.OWNER);
            });
            assertThat(res).anySatisfy(dto -> {
                assertThat(dto.userId()).isEqualTo(newUserId);
                assertThat(dto.role()).isEqualTo(HouseMemberRole.MEMBER);
            });
        }

        @Test
        @DisplayName("throws TENANT_GROUP_NOT_FOUND when group missing")
        void groupMissing() {
            when(tenantGroupRepository.findByHouseId(houseId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getMembers(houseId))
                    .isInstanceOf(HouseException.class)
                    .satisfies(ex -> assertThat(((HouseException) ex).getCode())
                            .isEqualTo(HouseErrorCode.TENANT_GROUP_NOT_FOUND));
        }

        @Test
        @DisplayName("returns empty list when no members")
        void empty() {
            when(tenantGroupRepository.findByHouseId(houseId)).thenReturn(Optional.of(group));
            when(tenantMemberRepository.findByTenantGroupId(tenantGroupId)).thenReturn(List.of());

            assertThat(service.getMembers(houseId)).isEmpty();
        }
    }
}
