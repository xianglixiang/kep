package com.kep.shared.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.TenantId;

/**
 * 所有租户隔离实体的基类。@TenantId 让 Hibernate 在 INSERT 时自动写入当前租户、
 * 在 SELECT 时自动追加 tenant_id 条件。子类无需、也不应手写 tenant 过滤。
 */
@MappedSuperclass
public abstract class TenantAwareEntity {

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    public String getTenantId() {
        return tenantId;
    }
}
