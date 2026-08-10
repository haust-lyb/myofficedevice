package com.chuangyi.myofficedevice.topology;

import com.chuangyi.myofficedevice.dto.Result;
import com.fasterxml.jackson.databind.JsonNode;
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
    public Result<JsonNode> get() {
        return Result.ok(topologyService.get());
    }

    @PutMapping
    public Result<TopologyService.SavedTopology> save(@RequestBody JsonNode topology) {
        return Result.ok(topologyService.save(topology));
    }
}
