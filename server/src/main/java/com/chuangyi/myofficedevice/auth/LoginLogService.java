package com.chuangyi.myofficedevice.auth;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LoginLogService {
    private final LoginLogRepository repository;

    public LoginLogService(LoginLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(Long userId, String username, LoginStatus status, String ipAddress, String userAgent) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUsername(trim(username, 64, "未知账号"));
        log.setStatus(status);
        log.setIpAddress(trim(ipAddress, 64, "未知"));
        log.setUserAgent(trim(userAgent, 500, ""));
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public LoginLogPage list(int requestedPage, int requestedSize) {
        int page = Math.max(0, requestedPage);
        int size = Math.max(1, Math.min(requestedSize, 100));
        var result = repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        List<LoginLogView> content = result.stream()
                .map(log -> new LoginLogView(log.getId(), log.getUserId(), log.getUsername(), log.getStatus(),
                        log.getIpAddress(), log.getUserAgent(), log.getCreatedAt()))
                .toList();
        return new LoginLogPage(content, result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.hasPrevious(), result.hasNext());
    }

    private String trim(String value, int maxLength, String fallback) {
        String result = value == null || value.isBlank() ? fallback : value.trim();
        return result.length() <= maxLength ? result : result.substring(0, maxLength);
    }

    public record LoginLogView(Long id, Long userId, String username, LoginStatus status,
                               String ipAddress, String userAgent, java.time.LocalDateTime createdAt) {}
    public record LoginLogPage(List<LoginLogView> content, int page, int size, long totalElements,
                               int totalPages, boolean hasPrevious, boolean hasNext) {}
}
