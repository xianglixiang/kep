package com.kep.catalog;

import com.kep.shared.tenant.TenantContext;
import com.kep.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** 默认剖面（启用 JPA），注入真实 JPA 适配器，验证 Hibernate 自动追加 tenant_id 过滤。 */
class TenantIsolationIT extends IntegrationTest {

    @Autowired CatalogNodeStore store;   // 默认剖面下解析为 JpaCatalogNodeStore

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void hibernate_auto_filters_by_current_tenant() {
        TenantContext.set("tenant-a");
        store.save(CatalogNode.folder(null, "A的根", "/"));

        TenantContext.set("tenant-b");
        store.save(CatalogNode.folder(null, "B的根", "/"));

        assertThat(store.findByParent(null)).extracting(CatalogNode::getName).containsExactly("B的根");

        TenantContext.set("tenant-a");
        assertThat(store.findByParent(null)).extracting(CatalogNode::getName).containsExactly("A的根");
    }
}
