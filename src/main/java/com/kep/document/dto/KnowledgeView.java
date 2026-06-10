package com.kep.document.dto;

import com.kep.document.Knowledge;

import java.time.OffsetDateTime;

public record KnowledgeView(
    Long id, Long catalogNodeId, String title, String docType, String status,
    KnowledgeVersionView currentVersion, OffsetDateTime createdAt
) {
    public static KnowledgeView from(Knowledge k, KnowledgeVersionView v) {
        return new KnowledgeView(k.getId(), k.getCatalogNodeId(), k.getTitle(),
            k.getDocType(), k.getStatus(), v, k.getCreatedAt());
    }
}
