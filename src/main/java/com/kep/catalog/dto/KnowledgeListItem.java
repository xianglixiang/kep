package com.kep.catalog.dto;

import com.kep.document.api.KnowledgeView;

import java.time.OffsetDateTime;

public record KnowledgeListItem(
    Long id, String title, String docType, String status,
    Long currentVersionId, OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static KnowledgeListItem from(KnowledgeView v) {
        return new KnowledgeListItem(
            v.id(), v.title(), v.docType(), v.status(),
            v.currentVersionId(), v.createdAt(), v.updatedAt()
        );
    }
}
