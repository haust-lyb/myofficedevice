package com.chuangyi.myofficedevice.topology;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface TopologyDocumentRepository extends JpaRepository<TopologyDocument, Long> {
    @Modifying
    @Query("""
            update TopologyDocument document
            set document.encryptedPayload = :encryptedPayload,
                document.updatedAt = :updatedAt,
                document.version = coalesce(document.version, 0) + 1
            where document.id = :id and coalesce(document.version, 0) = :expectedVersion
            """)
    int updateIfVersionMatches(
            @Param("id") Long id,
            @Param("expectedVersion") long expectedVersion,
            @Param("encryptedPayload") String encryptedPayload,
            @Param("updatedAt") LocalDateTime updatedAt);
}
