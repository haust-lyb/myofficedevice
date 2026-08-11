package com.chuangyi.myofficedevice.auth;

import com.chuangyi.myofficedevice.dto.Result;
import com.chuangyi.myofficedevice.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/login-logs")
public class LoginLogController {
    private final LoginLogService service;

    public LoginLogController(LoginLogService service) {
        this.service = service;
    }

    @GetMapping
    public Result<LoginLogService.LoginLogPage> list(HttpServletRequest request,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        AuthService.CurrentUser user = (AuthService.CurrentUser) request.getAttribute("currentUser");
        if (user == null || !user.canManageSystem()) throw new BusinessException(403, "无权查看登录日志");
        return Result.ok(service.list(page, size));
    }
}
