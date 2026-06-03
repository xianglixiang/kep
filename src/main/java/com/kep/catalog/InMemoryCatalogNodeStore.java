package com.kep.catalog;

import com.kep.shared.tenant.TenantContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存目录存储。用 TenantContext 当前租户作分区键，复刻 Hibernate @TenantId 的隔离语义，
 * 使快速测试与 local-mock 运行无需数据库即反映真实的跨租户不可见行为。
 *
 * 作为无数据源时的回退实现：DataSource 不存在时即生效，覆盖三种场景：
 *   1) @ActiveProfiles("local-mock")（DataSourceAutoConfiguration 被排除）
 *   2) @WebMvcTest 切片（无 JDBC 自动配置）
 *   3) 任何未配置数据源的环境
 */
@Component
@ConditionalOnMissingBean(DataSource.class)
class InMemoryCatalogNodeStore implements CatalogNodeStore {

    private final Map<String, Map<Long, CatalogNode>> byTenant = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong();

    private String tenant() {
        String t = TenantContext.get();
        return t != null ? t : "__unassigned__";
    }

    @Override
    public CatalogNode save(CatalogNode node) {
        node.assignId(seq.incrementAndGet());
        byTenant.computeIfAbsent(tenant(), k -> new LinkedHashMap<>()).put(node.getId(), node);
        return node;
    }

    @Override
    public List<CatalogNode> findByParent(Long parentId) {
        return byTenant.getOrDefault(tenant(), Map.of()).values().stream()
            .filter(n -> Objects.equals(n.getParentId(), parentId))
            .sorted(Comparator.comparingInt(CatalogNode::getSort))
            .toList();
    }
}
