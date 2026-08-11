package com.chuangyi.myofficedevice.topology;

import com.chuangyi.myofficedevice.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TopologyService {
    private static final long DOCUMENT_ID = 1L;
    private final TopologyDocumentRepository repository;
    private final TopologyCrypto crypto;
    private final ObjectMapper objectMapper;

    public TopologyService(TopologyDocumentRepository repository, TopologyCrypto crypto, ObjectMapper objectMapper) {
        this.repository = repository;
        this.crypto = crypto;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public TopologyResponse get() {
        return repository.findById(DOCUMENT_ID)
                .map(document -> new TopologyResponse(
                        parse(crypto.decrypt(document.getEncryptedPayload())),
                        versionOf(document)))
                .orElseGet(() -> new TopologyResponse(emptyTopology(), 0));
    }

    @Transactional
    public SavedTopology save(JsonNode topology, Long expectedVersion) {
        validate(topology);
        if (expectedVersion == null || expectedVersion < 0) {
            throw new IllegalArgumentException("提交拓扑时必须提供有效版本号");
        }

        LocalDateTime updatedAt = LocalDateTime.now();
        String encryptedPayload = crypto.encrypt(topology.toString());
        TopologyDocument document = repository.findById(DOCUMENT_ID).orElse(null);

        if (document == null) {
            if (expectedVersion != 0) throw conflict();
            document = new TopologyDocument();
            document.setId(DOCUMENT_ID);
            document.setEncryptedPayload(encryptedPayload);
            document.setUpdatedAt(updatedAt);
            document.setVersion(1L);
            repository.save(document);
            return new SavedTopology(updatedAt, 1);
        }

        if (repository.updateIfVersionMatches(DOCUMENT_ID, expectedVersion, encryptedPayload, updatedAt) != 1) {
            throw conflict();
        }
        return new SavedTopology(updatedAt, expectedVersion + 1);
    }

    @Transactional(readOnly = true)
    public ExportedTopology exportTopology() {
        TopologyResponse current = get();
        return new ExportedTopology("officemesh-topology", 1, LocalDateTime.now(), current.version(), current.topology());
    }

    @Transactional
    public SavedTopology importTopology(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            throw new BusinessException(400, "导入文件不是有效的 JSON 对象");
        }
        if (payload.has("format")
                && !"officemesh-topology".equals(payload.path("format").asText())
                && !"netdesk-topology".equals(payload.path("format").asText())) {
            throw new BusinessException(400, "不支持此备份文件格式");
        }
        if (payload.path("schemaVersion").asInt(1) > 1) {
            throw new BusinessException(400, "备份文件版本过新，请升级 OfficeMesh 后再导入");
        }

        JsonNode topology = payload.has("topology") ? payload.get("topology") : payload;
        try {
            validate(topology);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, exception.getMessage());
        }
        return save(topology, get().version());
    }

    private void validate(JsonNode topology) {
        if (topology == null || !topology.isObject() || !topology.path("nodes").isArray() || !topology.path("edges").isArray()) {
            throw new IllegalArgumentException("拓扑数据必须包含 nodes 和 edges 数组");
        }
    }

    private JsonNode parse(String value) {
        try { return objectMapper.readTree(value); }
        catch (Exception exception) { throw new IllegalStateException("Stored topology JSON is invalid", exception); }
    }

    private JsonNode emptyTopology() {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode nodes = result.putArray("nodes");
        ArrayNode edges = result.putArray("edges");
        return result;
    }

    private long versionOf(TopologyDocument document) {
        // Databases created before this field was introduced may contain NULL.
        return document.getVersion() == null ? 0 : document.getVersion();
    }

    private BusinessException conflict() {
        return new BusinessException(409, "拓扑已被其他用户修改，请刷新后再编辑");
    }

    public record TopologyResponse(JsonNode topology, long version) {}
    public record SavedTopology(LocalDateTime updatedAt, long version) {}
    public record ExportedTopology(String format, int schemaVersion, LocalDateTime exportedAt,
                                   long topologyVersion, JsonNode topology) {}
}
