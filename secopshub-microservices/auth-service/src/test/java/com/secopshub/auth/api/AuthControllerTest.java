package com.secopshub.auth.api;

import com.secopshub.auth.dto.LoginRequest;
import com.secopshub.auth.dto.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtEncoder jwtEncoder;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authenticationManager, jwtEncoder, 1800);
    }

    @Test
    void loginIssuesTokenAndReturnsSortedRoleNamesWithoutRolePrefix() {
        String username = "manager@secopshub.local";
        Authentication authentication = new UsernamePasswordAuthenticationToken(username, "unused",
                List.of(new SimpleGrantedAuthority("ROLE_INCIDENT_MANAGER"),
                        new SimpleGrantedAuthority("ROLE_AUDITOR")));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("signed.jwt.value")
                .header("alg", "HS256")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(1800))
                .subject(username)
                .build();
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

        LoginResponse response = controller.login(new LoginRequest(username, "Manager@123"));

        assertEquals("Bearer", response.tokenType());
        assertEquals("signed.jwt.value", response.accessToken());
        assertEquals(1800, response.expiresIn());
        assertEquals(username, response.username());
        assertEquals(List.of("AUDITOR", "INCIDENT_MANAGER"), response.roles());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }

    @Test
    void loginPropagatesAuthenticationFailureForInvalidCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> controller.login(new LoginRequest("manager@secopshub.local", "wrong-password")));
        verifyNoInteractions(jwtEncoder);
    }

    @Test
    void healthEndpointReportsServiceStatus() {
        assertEquals("auth-service", controller.health().get("service"));
        assertEquals("UP", controller.health().get("status"));
    }
}
