package com.secopshub.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class GatewaySecurityConfig {
    @Bean
    SecretKey jwtSecretKey(@Value("${security.jwt.secret-base64}") String secretBase64) {
        byte[] keyBytes = Base64.getDecoder().decode(secretBase64);
        if (keyBytes.length < 32) throw new IllegalStateException("JWT secret must decode to at least 32 bytes");
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    ReactiveJwtDecoder reactiveJwtDecoder(SecretKey jwtSecretKey) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("secopshub-auth"));
        return decoder;
    }

    @Bean
    Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
            Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/v1/auth/**", "/api/v1/health", "/api/v1/alerts/health", "/actuator/health").permitAll()
                        .pathMatchers(HttpMethod.PATCH, "/api/v1/incidents/*/severity").hasRole("INCIDENT_MANAGER")
                        .pathMatchers(HttpMethod.PATCH, "/api/v1/incidents/*/status").hasAnyRole("SOC_ANALYST", "INCIDENT_MANAGER")
                        .pathMatchers(HttpMethod.POST, "/api/v1/incidents", "/api/v1/incidents/*/relationships").hasAnyRole("SOC_ANALYST", "INCIDENT_MANAGER")
                        .pathMatchers(HttpMethod.GET, "/api/v1/reports/**").hasAnyRole("SOC_ANALYST", "INCIDENT_MANAGER", "AUDITOR")
                        .pathMatchers(HttpMethod.GET, "/api/v1/reference/**").hasAnyRole("SOC_ANALYST", "INCIDENT_MANAGER", "AUDITOR")
                        .pathMatchers(HttpMethod.GET, "/api/v1/incidents/**").hasAnyRole("SOC_ANALYST", "INCIDENT_MANAGER", "AUDITOR")
                        .pathMatchers(HttpMethod.POST, "/api/v1/alerts").hasAnyRole("SYSTEM_INTEGRATION", "SOC_ANALYST", "INCIDENT_MANAGER")
                        .pathMatchers(HttpMethod.GET, "/api/v1/alerts/**").hasAnyRole("SYSTEM_INTEGRATION", "SOC_ANALYST", "INCIDENT_MANAGER", "AUDITOR")
                        .pathMatchers("/api/v1/**").denyAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }
}
