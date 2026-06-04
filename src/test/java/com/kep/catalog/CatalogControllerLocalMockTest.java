package com.kep.catalog;

import com.kep.permission.api.AclRow;
import com.kep.permission.api.Subject;
import com.kep.permission.port.InMemoryAuthPort;
import com.kep.shared.security.SecurityFilter;
import com.kep.shared.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("local-mock")
@AutoConfigureMockMvc
class CatalogControllerLocalMockTest {

    @Autowired MockMvc mvc;
    @Autowired CatalogService catalogService;
    @Autowired InMemoryAuthPort authPort;
    @Autowired InMemoryCatalogNodeStore inMemoryStore;

    @Test
    void create_and_list_node_with_auth() throws Exception {
        // 引导 tenant-x 根节点（id=1）
        catalogService.bootstrapRoot("tenant-x");

        // InMemoryAuthPort 跟 InMemoryCatalogNodeStore 是两份独立的内存映射；
        // 真实生产由 JPA 共享同一张 catalog_node 表自然一致，local-mock 需手动同步：
        // 把 root 节点的 path 灌进 path port，鉴权算法才能算出祖先 id。
        authPort.addCatalogNode(1L, "/1/");

        // 给 user 1 在根节点 (id=1) 配 READ + WRITE
        authPort.addAcl("tenant-x", new AclRow(1L, Subject.user(1L), "READ", "GRANT"));
        authPort.addAcl("tenant-x", new AclRow(1L, Subject.user(1L), "WRITE", "GRANT"));

        // create 子节点
        mvc.perform(post("/api/catalog/nodes")
                .header(SecurityFilter.TENANT_HEADER, "tenant-x")
                .header(SecurityFilter.USER_HEADER, "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parentId\":1,\"name\":\"产品\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.success").value(true))
           .andExpect(jsonPath("$.data.name").value("产品"));

        // 把新建子节点的 path 同步到 InMemoryAuthPort（InMemoryCatalogNodeStore 已算好 path）
        TenantContext.set("tenant-x");
        for (CatalogNode c : inMemoryStore.findByParent(1L)) {
            authPort.addCatalogNode(c.getId(), c.getPath());
        }

        // list 看到 1 个
        mvc.perform(get("/api/catalog/nodes?parentId=1")
                .header(SecurityFilter.TENANT_HEADER, "tenant-x")
                .header(SecurityFilter.USER_HEADER, "1"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.length()").value(1))
           .andExpect(jsonPath("$.data[0].name").value("产品"));
    }

    @Test
    void unauthenticated_returns_401() throws Exception {
        mvc.perform(get("/api/catalog/nodes?parentId=1"))
           .andExpect(status().isUnauthorized());
    }
}
