package com.secopshub.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutes {
    @Bean
    public RouteLocator secopsRoutes(RouteLocatorBuilder builder,
                                     @Value("${secops.services.incident-uri:http://localhost:8081}") String incidentServiceUri,
                                     @Value("${secops.services.alert-uri:http://localhost:8082}") String alertIngestionServiceUri,
                                     @Value("${secops.services.auth-uri:http://localhost:8083}") String authServiceUri) {
        return builder.routes()
                .route("auth-service", route -> route
                        .path("/api/v1/auth", "/api/v1/auth/**")
                        .uri(authServiceUri))
                .route("alert-ingestion-service", route -> route
                        .path("/api/v1/alerts", "/api/v1/alerts/**")
                        .uri(alertIngestionServiceUri))
                .route("incident-service", route -> route
                        .path("/api/v1/incidents/**", "/api/v1/reports/**", "/api/v1/reference/**", "/api/v1/health")
                        .uri(incidentServiceUri))
                .build();
    }
}
