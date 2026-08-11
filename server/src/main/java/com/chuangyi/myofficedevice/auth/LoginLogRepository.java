package com.chuangyi.myofficedevice.auth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
    Page<LoginLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
