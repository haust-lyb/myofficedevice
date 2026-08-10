package com.chuangyi.myofficedevice.auth;

import com.chuangyi.myofficedevice.exception.BusinessException;
import com.chuangyi.myofficedevice.user.UserAccount;
import com.chuangyi.myofficedevice.user.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public AuthService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Value("${app.auth.token-hours:12}")
    private long tokenHours;

    public LoginResult login(String username, String password) {
        UserAccount user = userAccountRepository.findByUsername(username.trim())
                .filter(UserAccount::isEnabled)
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        String token = UUID.randomUUID() + "." + UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(tokenHours, ChronoUnit.HOURS);
        sessions.put(token, new Session(user.getId(), expiresAt));
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
        return new CurrentUser(user.getId(), user.getUsername(), user.getDisplayName());
    }

    private record Session(Long userId, Instant expiresAt) {}
    public record CurrentUser(Long id, String username, String displayName) {}
    public record LoginResult(String token, Instant expiresAt, CurrentUser user) {}
}
