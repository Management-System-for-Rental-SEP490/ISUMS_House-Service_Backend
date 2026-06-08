package com.isums.houseservice.configurations;

import com.isums.houseservice.infrastructures.grpcs.UserClientsGrpc;
import com.isums.userservice.grpc.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RemoteRoleJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserClientsGrpc userClientsGrpc;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        try {
            UserResponse response = userClientsGrpc.getUserIdAndRoleByKeyCloakId(jwt.getSubject());

            List<GrantedAuthority> authorities = response.getRolesList().stream()
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.trim().toUpperCase()))
                    .toList();

            return new JwtAuthenticationToken(jwt, authorities);

        } catch (Exception e) {
            log.warn("Failed to fetch roles for keycloakId={}, defaulting to empty. Error: {}",
                    jwt.getSubject(), e.getMessage());
            return new JwtAuthenticationToken(jwt, List.of());
        }
    }
}
