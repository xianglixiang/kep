package com.kep.document.api;

import com.kep.document.Knowledge;

import java.time.OffsetDateTime;

/**
 * 对外暴露的 Knowledge 视图 (M3 知识地图用)。
 * 内部 catalog 模块不能直接引用 {@link Knowledge} 实体 (modulith 边界),
 * 由调用方在 document 模块内完成 Knowledge -> KnowledgeView 的转换。
 */
public record KnowledgeView(
    Long id, String title, String docType, String status,
    Long currentVersionId, OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static KnowledgeView from(Knowledge k) {
        return new KnowledgeView(
            k.getId(), k.getTitle(), k.getDocType(), k.getStatus(),
            k.getCurrentVersionId(), k.getCreatedAt(), k.getUpdatedAt()
        );
    }
}
