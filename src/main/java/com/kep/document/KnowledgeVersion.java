package com.kep.document;

import com.kep.shared.jpa.TenantAwareEntity;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "knowledge_version",
       uniqueConstraints = @UniqueConstraint(columnNames = {"knowledge_id", "version_no"}))
public class KnowledgeVersion extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "knowledge_id", nullable = false)
    private Long knowledgeId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(name = "content_richtext", nullable = false, columnDefinition = "TEXT")
    private String contentRichtext;

    @Column(name = "original_file_key", length = 500)
    private String originalFileKey;

    @Column(name = "file_format", length = 20)
    private String fileFormat;

    @Column(name = "change_type", nullable = false, length = 20)
    private String changeType;

    @Column(name = "parent_version_id")
    private Long parentVersionId;

    @Column(name = "editor_id", nullable = false)
    private Long editorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String comment;

    protected KnowledgeVersion() {}

    public static KnowledgeVersion create(Long knowledgeId, int versionNo, String content,
                                          String originalFileKey, String fileFormat, Long editorId) {
        KnowledgeVersion v = new KnowledgeVersion();
        v.knowledgeId = knowledgeId;
        v.versionNo = versionNo;
        v.contentRichtext = content;
        v.originalFileKey = originalFileKey;
        v.fileFormat = fileFormat;
        v.changeType = "CREATE";
        v.editorId = editorId;
        v.createdAt = OffsetDateTime.now();
        return v;
    }

    public static KnowledgeVersion update(Long knowledgeId, int versionNo, String content,
                                          String originalFileKey, String fileFormat,
                                          Long parentVersionId, Long editorId) {
        KnowledgeVersion v = new KnowledgeVersion();
        v.knowledgeId = knowledgeId;
        v.versionNo = versionNo;
        v.contentRichtext = content;
        v.originalFileKey = originalFileKey;
        v.fileFormat = fileFormat;
        v.changeType = "UPDATE";
        v.parentVersionId = parentVersionId;
        v.editorId = editorId;
        v.createdAt = OffsetDateTime.now();
        return v;
    }

    public Long getId() { return id; }
    public Long getKnowledgeId() { return knowledgeId; }
    public int getVersionNo() { return versionNo; }
    public String getContentRichtext() { return contentRichtext; }
    public String getOriginalFileKey() { return originalFileKey; }
    public String getFileFormat() { return fileFormat; }
    public String getChangeType() { return changeType; }
    public Long getParentVersionId() { return parentVersionId; }
    public Long getEditorId() { return editorId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getComment() { return comment; }
}
