package com.chuangyi.myofficedevice.config;

import com.chuangyi.myofficedevice.auth.AuthService;
import com.chuangyi.myofficedevice.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(401, "请先登录");
        }
        String token = authorization.substring(7).trim();
        request.setAttribute("currentUser", authService.authenticate(token));
        request.setAttribute("accessToken", token);
        return true;
    }
}
