package com.kep.catalog;

import com.kep.catalog.dto.CreateNodeRequest;
import com.kep.permission.api.Permission;
import com.kep.permission.api.PermissionChecker;
import com.kep.shared.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogServiceTest {

    /** 单元测试桩：放行一切鉴权检查，聚焦 catalog 自身行为（租户分区、子节点归属）。 */
    private static final PermissionChecker ALLOW_ALL = new PermissionChecker() {
        @Override public void check(Long userId, Long catalogNodeId, Permission required) {}
        @Override public boolean has(Long userId, Long catalogNodeId, Permission required) { return true; }
    };

    private final CatalogService service = new CatalogService(new InMemoryCatalogNodeStore(), ALLOW_ALL);

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void bootstrap_then_create_child_then_list_for_same_tenant() {
        service.bootstrapRoot("tenant-a");        // root id=1
        TenantContext.set("tenant-a");
        service.create(1L, new CreateNodeRequest(1L, "产品"));

        assertThat(service.listChildren(1L, 1L)).extracting(v -> v.name()).containsExactly("产品");
    }

    @Test
    void nodes_are_isolated_between_tenants() {
        service.bootstrapRoot("tenant-a");        // id=1
        service.bootstrapRoot("tenant-b");        // id=2

        TenantContext.set("tenant-a");
        service.create(1L, new CreateNodeRequest(1L, "A的子"));

        TenantContext.set("tenant-b");
        service.create(1L, new CreateNodeRequest(2L, "B的子"));

        TenantContext.set("tenant-a");
        assertThat(service.listChildren(1L, 1L)).extracting(v -> v.name()).containsExactly("A的子");

        TenantContext.set("tenant-b");
        assertThat(service.listChildren(1L, 2L)).extracting(v -> v.name()).containsExactly("B的子");
    }
}
