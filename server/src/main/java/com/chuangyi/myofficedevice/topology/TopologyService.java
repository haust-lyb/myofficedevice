package com.chuangyi.myofficedevice.topology;

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
    public JsonNode get() {
        return repository.findById(DOCUMENT_ID)
                .map(document -> parse(crypto.decrypt(document.getEncryptedPayload())))
                .orElseGet(this::emptyTopology);
    }

    @Transactional
    public SavedTopology save(JsonNode topology) {
        validate(topology);
        TopologyDocument document = repository.findById(DOCUMENT_ID).orElseGet(TopologyDocument::new);
        document.setId(DOCUMENT_ID);
        document.setEncryptedPayload(crypto.encrypt(topology.toString()));
        document.setUpdatedAt(LocalDateTime.now());
        repository.save(document);
        return new SavedTopology(document.getUpdatedAt());
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

    public record SavedTopology(LocalDateTime updatedAt) {}
}
