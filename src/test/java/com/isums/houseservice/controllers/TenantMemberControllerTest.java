package com.isums.houseservice.controllers;

import com.isums.houseservice.domains.dtos.TenantMemberDto;
import com.isums.houseservice.domains.emuns.HouseMemberRole;
import com.isums.houseservice.exceptions.GlobalExceptionHandler;
import com.isums.houseservice.exceptions.HouseErrorCode;
import com.isums.houseservice.exceptions.HouseException;
import com.isums.houseservice.infrastructures.abstracts.TenantMemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantMemberController")
class TenantMemberControllerTest {

    @Mock private TenantMemberService service;

    @InjectMocks private TenantMemberController controller;

    private MockMvc mvc;
    private UUID requesterId;

    @BeforeEach
    void setUp() {
        requesterId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .subject(requesterId.toString()).build();

        HandlerMethodArgumentResolver jwtResolver = new HandlerMethodArgumentResolver() {
            @Override public boolean supportsParameter(MethodParameter p) {
                return Jwt.class.equals(p.getParameterType());
            }
            @Override public Object resolveArgument(MethodParameter p, ModelAndViewContainer m,
                                                    NativeWebRequest w, WebDataBinderFactory b) {
                return jwt;
            }
        };

        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(jwtResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET returns list of members")
    void getMembers() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID uid = UUID.randomUUID();
        TenantMemberDto dto = new TenantMemberDto(uid, HouseMemberRole.MEMBER, true, Instant.now());

        when(service.getMembers(houseId)).thenReturn(List.of(dto));

        mvc.perform(get("/api/houses/{hid}/members", houseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(uid.toString()))
                .andExpect(jsonPath("$.data[0].role").value("MEMBER"));
    }

    @Test
    @DisplayName("POST passes requesterId from Jwt subject")
    void addMember() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mvc.perform(post("/api/houses/{hid}/members/{uid}", houseId, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Member added successfully"));

        verify(service).addMember(houseId, requesterId, userId);
    }

    @Test
    @DisplayName("DELETE passes requesterId from Jwt subject")
    void removeMember() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        mvc.perform(delete("/api/houses/{hid}/members/{uid}", houseId, userId))
                .andExpect(status().isOk());

        verify(service).removeMember(houseId, requesterId, userId);
    }

    @Test
    @DisplayName("POST surfaces HouseException as HTTP status from the code")
    void conflictSurfaces() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        doThrow(new HouseException(HouseErrorCode.MEMBER_ALREADY_EXISTS))
                .when(service).addMember(houseId, requesterId, userId);

        mvc.perform(post("/api/houses/{hid}/members/{uid}", houseId, userId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0].code").value("MEMBER_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("DELETE surfaces NOT_HOUSE_OWNER as 403")
    void notOwner() throws Exception {
        UUID houseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        doThrow(new HouseException(HouseErrorCode.NOT_HOUSE_OWNER))
                .when(service).removeMember(houseId, requesterId, userId);

        mvc.perform(delete("/api/houses/{hid}/members/{uid}", houseId, userId))
                .andExpect(status().isForbidden());
    }
}
