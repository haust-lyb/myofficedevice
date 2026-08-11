package com.chuangyi.myofficedevice.auth;

import com.chuangyi.myofficedevice.exception.BusinessException;
import com.chuangyi.myofficedevice.user.UserAccount;
import com.chuangyi.myofficedevice.user.UserAccountRepository;
import com.chuangyi.myofficedevice.user.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 10;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginLogService loginLogService;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public AuthService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder,
                       LoginLogService loginLogService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginLogService = loginLogService;
    }

    @Value("${app.auth.token-hours:12}")
    private long tokenHours;

    public synchronized LoginResult login(String username, String password, LoginContext context) {
        String normalizedUsername = username.trim();
        UserAccount user = userAccountRepository.findByUsername(normalizedUsername).orElse(null);
        if (user == null) {
            loginLogService.record(null, normalizedUsername, LoginStatus.BAD_CREDENTIALS,
                    context.ipAddress(), context.userAgent());
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!user.isEnabled()) {
            loginLogService.record(user.getId(), normalizedUsername, LoginStatus.DISABLED,
                    context.ipAddress(), context.userAgent());
            throw new BusinessException(401, "用户名或密码错误");
        }

        LocalDateTime now = LocalDateTime.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            loginLogService.record(user.getId(), normalizedUsername, LoginStatus.LOCKED,
                    context.ipAddress(), context.userAgent());
            throw new BusinessException(423, "账号已锁定，请在10分钟后重试");
        }
        if (user.getLockedUntil() != null) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userAccountRepository.saveAndFlush(user);
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            int failedAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failedAttempts);
            LoginStatus status = LoginStatus.BAD_CREDENTIALS;
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
                status = LoginStatus.LOCKED;
            }
            userAccountRepository.saveAndFlush(user);
            loginLogService.record(user.getId(), normalizedUsername, status,
                    context.ipAddress(), context.userAgent());
            if (status == LoginStatus.LOCKED) {
                throw new BusinessException(423, "密码连续错误5次，账号已锁定10分钟");
            }
            throw new BusinessException(401, "用户名或密码错误");
        }

        if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userAccountRepository.saveAndFlush(user);
        }
        String token = UUID.randomUUID() + "." + UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(tokenHours, ChronoUnit.HOURS);
        sessions.put(token, new Session(user.getId(), expiresAt));
        loginLogService.record(user.getId(), normalizedUsername, LoginStatus.SUCCESS,
                context.ipAddress(), context.userAgent());
        return new LoginResult(token, expiresAt, toCurrentUser(user));
    }

    public CurrentUser authenticate(String token) {
        Session session = sessions.get(token);
        if (session == null || session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            throw new BusinessException(401, "登录状态已失效，请重新登录");
        }
        UserAccount user = userAccountRepository.findById(session.userId())
                .filter(UserAccount::isEnabled)
                .orElseThrow(() -> new BusinessException(401, "账号不可用"));
        return toCurrentUser(user);
    }

    public void logout(String token) {
        sessions.remove(token);
    }

    private CurrentUser toCurrentUser(UserAccount user) {
        UserRole role = user.getRole();
        return new CurrentUser(user.getId(), user.getUsername(), user.getDisplayName(), role,
                role.canEditTopology(), role.canManageSystem());
    }

    private record Session(Long userId, Instant expiresAt) {}
    public record LoginContext(String ipAddress, String userAgent) {}
    public record CurrentUser(Long id, String username, String displayName, UserRole role,
                              boolean canEditTopology, boolean canManageSystem) {}
    public record LoginResult(String token, Instant expiresAt, CurrentUser user) {}
}
