package com.kep.document.dto;

import com.kep.document.EditLock;

import java.time.OffsetDateTime;

public record EditLockView(
    Long id, Long knowledgeId, Long holderId,
    OffsetDateTime acquiredAt, OffsetDateTime expiresAt
) {
    public static EditLockView from(EditLock l) {
        return new EditLockView(l.getId(), l.getKnowledgeId(), l.getHolderId(),
            l.getAcquiredAt(), l.getExpiresAt());
    }
}
