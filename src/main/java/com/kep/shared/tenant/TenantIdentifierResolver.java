package com.kep.shared.tenant;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 把 TenantContext 的当前租户提供给 Hibernate。
 * 对带 @TenantId 的实体：写入时自动落该值、查询时自动追加 tenant_id 过滤，业务代码无法绕过。
 * 注：local-mock 剖面排除 JPA，本 Bean 存在但不被 Hibernate 使用，无副作用。
 */
@Component
public class TenantIdentifierResolver
        implements CurrentTenantIdentifierResolver<String>, HibernatePropertiesCustomizer {

    /** 无租户上下文时（如系统任务/启动）使用的哨兵值，永不返回 null。 */
    public static final String UNASSIGNED = "__unassigned__";

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenantId = TenantContext.get();
        return tenantId != null ? tenantId : UNASSIGNED;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
