package com.chuangyi.myofficedevice.user;

import com.chuangyi.myofficedevice.auth.AuthService;
import com.chuangyi.myofficedevice.dto.Result;
import com.chuangyi.myofficedevice.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserManagementController {
    private final UserManagementService service;

    public UserManagementController(UserManagementService service) {
        this.service = service;
    }

    @GetMapping
    public Result<List<UserManagementService.UserView>> list(HttpServletRequest servletRequest) {
        requireSuperAdmin(servletRequest);
        return Result.ok(service.list());
    }

    @PostMapping
    public Result<UserManagementService.UserView> create(HttpServletRequest servletRequest,
                                                          @Valid @RequestBody CreateUserRequest request) {
        requireSuperAdmin(servletRequest);
        return Result.ok(service.create(new UserManagementService.CreateUser(
                request.username(), request.displayName(), request.password(), request.role())));
    }

    @PutMapping("/{id}")
    public Result<UserManagementService.UserView> update(HttpServletRequest servletRequest,
                                                          @PathVariable Long id,
                                                          @Valid @RequestBody UpdateUserRequest request) {
        requireSuperAdmin(servletRequest);
        return Result.ok(service.update(id, new UserManagementService.UpdateUser(
                request.displayName(), request.password(), request.role(), request.enabled())));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(HttpServletRequest servletRequest, @PathVariable Long id) {
        requireSuperAdmin(servletRequest);
        service.delete(id);
        return Result.ok();
    }

    private void requireSuperAdmin(HttpServletRequest request) {
        AuthService.CurrentUser user = (AuthService.CurrentUser) request.getAttribute("currentUser");
        if (user == null || !user.canManageSystem()) throw new BusinessException(403, "无权访问系统设置");
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(max = 80) String displayName,
            @NotBlank @Size(min = 6, max = 100) String password,
            @NotNull UserRole role) {}

    public record UpdateUserRequest(
            @NotBlank @Size(max = 80) String displayName,
            @Size(max = 100) String password,
            @NotNull UserRole role,
            boolean enabled) {}
}
