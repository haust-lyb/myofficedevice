package com.chuangyi.myofficedevice.auth;

import com.chuangyi.myofficedevice.dto.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<AuthService.LoginResult> login(@Valid @RequestBody LoginRequest request,
                                                  HttpServletRequest servletRequest) {
        return Result.ok(authService.login(request.username(), request.password(),
                new AuthService.LoginContext(clientIp(servletRequest), servletRequest.getHeader("User-Agent"))));
    }

    @GetMapping("/me")
    public Result<AuthService.CurrentUser> me(HttpServletRequest request) {
        return Result.ok((AuthService.CurrentUser) request.getAttribute("currentUser"));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        authService.logout((String) request.getAttribute("accessToken"));
        return Result.ok();
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",", 2)[0].trim();
        return request.getRemoteAddr();
    }
}
