package com.chuangyi.myofficedevice.user;

import com.chuangyi.myofficedevice.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserManagementService {
    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(UserAccountRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserView> list() {
        return repository.findAll().stream()
                .sorted((left, right) -> Long.compare(left.getId(), right.getId()))
                .map(this::toView)
                .toList();
    }

    @Transactional
    public UserView create(CreateUser request) {
        String username = request.username().trim();
        if (repository.findByUsername(username).isPresent()) {
            throw new BusinessException(409, "用户名已存在");
        }
        UserRole role = editableRole(request.role());
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setDisplayName(request.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setEnabled(true);
        return toView(repository.save(user));
    }

    @Transactional
    public UserView update(Long id, UpdateUser request) {
        UserAccount user = find(id);
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            user.setDisplayName(request.displayName().trim());
            user.setEnabled(true);
        } else {
            user.setDisplayName(request.displayName().trim());
            user.setRole(editableRole(request.role()));
            user.setEnabled(request.enabled());
        }
        if (request.password() != null && !request.password().isBlank()) {
            if (request.password().length() < 6) throw new BusinessException(400, "密码至少需要 6 位");
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return toView(repository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        UserAccount user = find(id);
        if (user.getRole() != UserRole.USER) {
            throw new BusinessException(400, "管理员账号不可删除，可先调整为普通用户");
        }
        repository.delete(user);
    }

    private UserRole editableRole(UserRole role) {
        if (role == null || role == UserRole.SUPER_ADMIN) {
            throw new BusinessException(400, "只能配置普通用户或管理员角色");
        }
        return role;
    }

    private UserAccount find(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }

    private UserView toView(UserAccount user) {
        return new UserView(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(),
                user.isEnabled(), user.getCreatedAt());
    }

    public record CreateUser(String username, String displayName, String password, UserRole role) {}
    public record UpdateUser(String displayName, String password, UserRole role, boolean enabled) {}
    public record UserView(Long id, String username, String displayName, UserRole role, boolean enabled,
                           java.time.LocalDateTime createdAt) {}
}
