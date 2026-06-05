package com.kep.document;

import com.kep.shared.jpa.TenantAwareEntity;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.OffsetDateTime;

@Entity
@Table(name = "edit_lock",
       uniqueConstraints = @UniqueConstraint(name = "uk_edit_lock_knowledge", columnNames = "knowledge_id"))
public class EditLock extends TenantAwareEntity {

    public static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "knowledge_id", nullable = false)
    private Long knowledgeId;

    @Column(name = "holder_id", nullable = false)
    private Long holderId;

    @Column(name = "acquired_at", nullable = false, updatable = false)
    private OffsetDateTime acquiredAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    protected EditLock() {}

    public static EditLock acquire(Long knowledgeId, Long holderId) {
        OffsetDateTime now = OffsetDateTime.now();
        EditLock lock = new EditLock();
        lock.knowledgeId = knowledgeId;
        lock.holderId = holderId;
        lock.acquiredAt = now;
        lock.expiresAt = now.plus(DEFAULT_TTL);
        return lock;
    }

    public static EditLock refresh(EditLock existing, Long holderId) {
        OffsetDateTime now = OffsetDateTime.now();
        existing.holderId = holderId;
        existing.acquiredAt = now;
        existing.expiresAt = now.plus(DEFAULT_TTL);
        return existing;
    }

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
    }

    public boolean isHeldBy(Long userId) {
        return userId != null && userId.equals(holderId);
    }

    public Long getId() { return id; }
    public Long getKnowledgeId() { return knowledgeId; }
    public Long getHolderId() { return holderId; }
    public OffsetDateTime getAcquiredAt() { return acquiredAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
}
