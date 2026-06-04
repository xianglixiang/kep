package com.kep.catalog;

import com.kep.catalog.dto.CreateNodeRequest;
import com.kep.shared.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogServiceTest {

    private final CatalogService service = new CatalogService(new InMemoryCatalogNodeStore());

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void create_then_list_returns_node_for_same_tenant() {
        TenantContext.set("tenant-a");
        service.create(new CreateNodeRequest(null, "产品"));

        assertThat(service.listChildren(null))
            .extracting(v -> v.name())
            .containsExactly("产品");
    }

    @Test
    void nodes_are_isolated_between_tenants() {
        TenantContext.set("tenant-a");
        service.create(new CreateNodeRequest(null, "A的根"));

        TenantContext.set("tenant-b");
        service.create(new CreateNodeRequest(null, "B的根"));

        assertThat(service.listChildren(null)).extracting(v -> v.name()).containsExactly("B的根");

        TenantContext.set("tenant-a");
        assertThat(service.listChildren(null)).extracting(v -> v.name()).containsExactly("A的根");
    }
}
