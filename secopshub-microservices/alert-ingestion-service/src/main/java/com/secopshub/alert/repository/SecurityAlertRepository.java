package com.secopshub.alert.repository;

import com.secopshub.alert.domain.SecurityAlert;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {
    Optional<SecurityAlert> findByAlertId(String alertId);
    Optional<SecurityAlert> findByExternalAlertId(String externalAlertId);
    java.util.List<SecurityAlert> findAllByOrderByCreatedAtDesc();
}
