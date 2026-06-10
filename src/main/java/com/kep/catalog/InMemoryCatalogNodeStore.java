package com.kep.catalog;

import com.kep.shared.tenant.TenantContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * local-mock 下的内存目录存储。用 TenantContext 当前租户作分区键，复刻 Hibernate @TenantId 的隔离语义。
 * M1 增强：save 时计算 path（'/parent-path/{id}/'）。
 */
@Component
@Profile("local-mock")
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

    @Override
    public List<CatalogNode> findAll() {
        return new ArrayList<>(byTenant.getOrDefault(tenant(), Map.of()).values());
    }

    @Override
    public Optional<CatalogNode> findById(Long id) {
        return Optional.ofNullable(byTenant.getOrDefault(tenant(), Map.of()).get(id));
    }

    @Override
    public List<CatalogNode> findAllById(List<Long> ids) {
        Map<Long, CatalogNode> map = byTenant.getOrDefault(tenant(), Map.of());
        List<CatalogNode> out = new ArrayList<>();
        for (Long id : ids) {
            CatalogNode n = map.get(id);
            if (n != null) out.add(n);
        }
        return out;
    }
}
