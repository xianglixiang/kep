package com.kep.catalog;

import com.kep.shared.tenant.TenantContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存目录存储。用 TenantContext 当前租户作分区键，复刻 Hibernate @TenantId 的隔离语义。
 * M1 增强：save 时计算 path（'/parent-path/{id}/'）。
 *
 * 作为 JpaCatalogNodeStore 的回退：JPA 端口的 {@code @ConditionalOnBean(CatalogNodeRepository.class)}
 * 在自动装配阶段因 bean 创建时序而无法命中（Spring 已知陷阱），故 M0 即靠此回退让 IT 也能跑通。
 * local-mock 模式下 DataSource 不存在，本 bean 自然命中。
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
        if (node.getId() == null) {
            node.assignId(seq.incrementAndGet());
        }
        // 算 path
        String parentPath = "/";
        if (node.getParentId() != null) {
            CatalogNode parent = byTenant.getOrDefault(tenant(), Map.of())
                .get(node.getParentId());
            if (parent != null) parentPath = parent.getPath();
        }
        node.assignPath(parentPath + node.getId() + "/");
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
