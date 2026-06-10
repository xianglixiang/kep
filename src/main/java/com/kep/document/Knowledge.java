package com.kep.document;

import com.kep.shared.jpa.TenantAwareEntity;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "knowledge")
public class Knowledge extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "catalog_node_id", nullable = false)
    private Long catalogNodeId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "doc_type", nullable = false, length = 20)
    private String docType = "WORD";

    @Column(nullable = false, length = 20)
    private String status = "PUBLISHED";

    @Column(name = "current_version_id")
    private Long currentVersionId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Knowledge() {}

    public static Knowledge newInstance(Long catalogNodeId, String title, Long createdBy) {
        Knowledge k = new Knowledge();
        k.catalogNodeId = catalogNodeId;
        k.title = title;
        k.createdBy = createdBy;
        k.createdAt = OffsetDateTime.now();
        k.updatedAt = k.createdAt;
        return k;
    }

    void setCurrentVersionId(Long id) { this.currentVersionId = id; this.updatedAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public Long getCatalogNodeId() { return catalogNodeId; }
    public String getTitle() { return title; }
    public String getDocType() { return docType; }
    public String getStatus() { return status; }
    public Long getCurrentVersionId() { return currentVersionId; }
    public Long getCreatedBy() { return createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
