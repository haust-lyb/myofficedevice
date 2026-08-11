package com.chuangyi.myofficedevice.topology;

import com.chuangyi.myofficedevice.auth.AuthService;
import com.chuangyi.myofficedevice.dto.Result;
import com.chuangyi.myofficedevice.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/topology")
public class TopologyController {
    private final TopologyService topologyService;

    public TopologyController(TopologyService topologyService) {
        this.topologyService = topologyService;
    }

    @GetMapping
    public Result<TopologyService.TopologyResponse> get() {
        return Result.ok(topologyService.get());
    }

    @PutMapping
    public Result<TopologyService.SavedTopology> save(HttpServletRequest servletRequest,
                                                       @RequestBody SaveTopologyRequest request) {
        AuthService.CurrentUser user = (AuthService.CurrentUser) servletRequest.getAttribute("currentUser");
        if (user == null || !user.canEditTopology()) throw new BusinessException(403, "当前账号只有查看权限");
        return Result.ok(topologyService.save(request.topology(), request.version()));
    }

    public record SaveTopologyRequest(JsonNode topology, Long version) {}
}
