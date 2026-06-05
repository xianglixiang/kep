package com.kep.document.dto;

import com.kep.document.KnowledgeVersion;

import java.time.OffsetDateTime;

public record KnowledgeVersionView(
    Long id, int versionNo, String changeType, String contentRichtext,
    String fileFormat, Long editorId, OffsetDateTime createdAt
) {
    public static KnowledgeVersionView from(KnowledgeVersion v) {
        return new KnowledgeVersionView(v.getId(), v.getVersionNo(), v.getChangeType(),
            v.getContentRichtext(), v.getFileFormat(), v.getEditorId(), v.getCreatedAt());
    }
}
