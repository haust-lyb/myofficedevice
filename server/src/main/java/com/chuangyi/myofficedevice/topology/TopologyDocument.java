package com.chuangyi.myofficedevice.topology;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "topology_document")
public class TopologyDocument {
    @Id
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedPayload;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEncryptedPayload() { return encryptedPayload; }
    public void setEncryptedPayload(String encryptedPayload) { this.encryptedPayload = encryptedPayload; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
