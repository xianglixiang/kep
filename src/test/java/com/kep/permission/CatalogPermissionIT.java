package com.kep.permission;

import com.kep.catalog.CatalogNode;
import com.kep.catalog.CatalogNodeStore;
import com.kep.shared.security.SecurityFilter;
import com.kep.shared.tenant.TenantContext;
import com.kep.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HTTP 端到端鉴权演示（验收）：
 *  - WRITE 不足 → 403
 *  - 跨租户 → 403
 *  - 列表过滤: 不可见子节点不返回
 *  - 有 ACL 时正常工作
 */
@AutoConfigureMockMvc
class CatalogPermissionIT extends IntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired CatalogNodeStore store;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void cleanState() {
        // 测试间隔离: 清掉所有表（含 org_unit，否则显式 id=1L 插入会冲突）
        jdbc.execute("TRUNCATE TABLE acl, review_request, app_user, user_org, org_unit, " +
                     "catalog_node RESTART IDENTITY CASCADE");
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
        com.kep.shared.security.SecurityContext.clear();
    }

    @Test
    void create_returns_403_when_no_write_acl() throws Exception {
        seedBaseTenant();
        Long aliceId = seedUserAndOrg("alice", 1L);

        // alice 在 tenant-a 想 POST /api/catalog/nodes 但没有任何 ACL
        mvc.perform(post("/api/catalog/nodes")
                .header(SecurityFilter.TENANT_HEADER, "tenant-a")
                .header(SecurityFilter.USER_HEADER, String.valueOf(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parentId\":1,\"name\":\"hack\"}"))
           .andExpect(status().isForbidden());
    }

    @Test
    void create_succeeds_when_write_acl_present() throws Exception {
        seedBaseTenant();
        Long aliceId = seedUserAndOrg("alice", 1L);
        // 给 alice 在根节点 1 配 WRITE
        jdbc.update(
            "INSERT INTO acl (tenant_id, resource_type, resource_id, subject_type, subject_id, permission, effect) " +
            "VALUES ('tenant-a', 'CATALOG_NODE', 1, 'USER', ?, 'WRITE', 'GRANT')",
            aliceId);

        mvc.perform(post("/api/catalog/nodes")
                .header(SecurityFilter.TENANT_HEADER, "tenant-a")
                .header(SecurityFilter.USER_HEADER, String.valueOf(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parentId\":1,\"name\":\"产品\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.success").value(true))
           .andExpect(jsonPath("$.data.name").value("产品"));
    }

    @Test
    void cross_tenant_returns_403() throws Exception {
        seedBaseTenant();
        Long aliceId = seedUserAndOrg("alice", 1L);
        // ACL 只在 tenant-a
        jdbc.update(
            "INSERT INTO acl (tenant_id, resource_type, resource_id, subject_type, subject_id, permission, effect) " +
            "VALUES ('tenant-a', 'CATALOG_NODE', 1, 'USER', ?, 'WRITE', 'GRANT')",
            aliceId);

        // 同一 user 但请求头换到 tenant-b → 拒绝
        mvc.perform(post("/api/catalog/nodes")
                .header(SecurityFilter.TENANT_HEADER, "tenant-b")
                .header(SecurityFilter.USER_HEADER, String.valueOf(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"parentId\":1,\"name\":\"x\"}"))
           .andExpect(status().isForbidden());
    }

    @Test
    void list_filters_out_unreadable_children() throws Exception {
        seedBaseTenant();
        Long aliceId = seedUserAndOrg("alice", 1L);
        // 给 alice 在根节点 1 READ
        jdbc.update(
            "INSERT INTO acl (tenant_id, resource_type, resource_id, subject_type, subject_id, permission, effect) " +
            "VALUES ('tenant-a', 'CATALOG_NODE', 1, 'USER', ?, 'READ', 'GRANT')",
            aliceId);

        // 创建子节点 2（含 alice 可见的"产品"）和子节点 3（私密，仅 alice 不可见）。
        // IT 模式实际装配的是 InMemoryCatalogNodeStore（in-memory），所以也需要
        // 把子节点灌进内存 store，让 CatalogService.listChildren.findByParent 能拿到。
        // 注：算法对"无 ACL 的子节点"会回退到祖先 GRANT——单纯不给私密加 ACL，
        // 私密仍会因继承根的 GRANT 而对 alice 可见。
        // 正确的不可见语义是给私密加 DENY（DENY 覆盖父级 GRANT），这才符合"子节点 ACL"的设计意图。

        Long productId = insertNodeAndMemory("产品", 1L);
        Long privateId = insertNodeAndMemory("私密", 1L);

        // 给 alice READ on node 2 (产品)
        jdbc.update(
            "INSERT INTO acl (tenant_id, resource_type, resource_id, subject_type, subject_id, permission, effect) " +
            "VALUES ('tenant-a', 'CATALOG_NODE', ?, 'USER', ?, 'READ', 'GRANT')",
            productId, aliceId);
        // 给 alice DENY on node 3 (私密)——覆盖父级 GRANT
        jdbc.update(
            "INSERT INTO acl (tenant_id, resource_type, resource_id, subject_type, subject_id, permission, effect) " +
            "VALUES ('tenant-a', 'CATALOG_NODE', ?, 'USER', ?, 'READ', 'DENY')",
            privateId, aliceId);

        // list 应过滤掉"私密"
        mvc.perform(get("/api/catalog/nodes?parentId=1")
                .header(SecurityFilter.TENANT_HEADER, "tenant-a")
                .header(SecurityFilter.USER_HEADER, String.valueOf(aliceId)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.length()").value(1))
           .andExpect(jsonPath("$.data[0].name").value("产品"));
    }

    @Test
    void list_denied_when_no_read_acl_on_parent() throws Exception {
        seedBaseTenant();
        Long aliceId = seedUserAndOrg("alice", 1L);
        // 没有任何 ACL → 父节点 READ 失败 → 整个 list 403
        mvc.perform(get("/api/catalog/nodes?parentId=1")
                .header(SecurityFilter.TENANT_HEADER, "tenant-a")
                .header(SecurityFilter.USER_HEADER, String.valueOf(aliceId)))
           .andExpect(status().isForbidden());
    }

    // ---------- 种子数据辅助 ----------

    private void seedBaseTenant() {
        // 同时灌两份：DB（供鉴权端口查询）+ 内存 store（供 CatalogService.listChildren.findByParent）。
        // 原因：M0 已知坑——JpaCatalogNodeStore 的 @ConditionalOnBean 因 bean 时序不命中，
        // IT 模式下实际装配的是 InMemoryCatalogNodeStore（in-memory），不走 DB。
        // 而鉴权端口（JpaCatalogPathPort/JpaUserOrgPort/JpaAclPort）都是 JPA 直查 DB。
        // 这是 Task 8 catalog 接入时的遗留 wiring bug，不在 Task 10 修复范围。
        jdbc.update(
            "INSERT INTO catalog_node (tenant_id, parent_id, name, path) " +
            "OVERRIDING SYSTEM VALUE VALUES ('tenant-a', NULL, 'root', '/1/')");
        TenantContext.set("tenant-a");
        try {
            store.save(CatalogNode.folder(null, "root", "/"));
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 双写：DB 落库（鉴权端口能找到）+ 内存 store（listChildren.findByParent 能找到）。
     * DB 显式 id，内存 store 顺序 save 由 AtomicLong 派 1/2/3...，与 DB 同步。
     */
    private Long insertNodeAndMemory(String name, long parentId) {
        // 1) DB
        jdbc.update(
            "INSERT INTO catalog_node (tenant_id, parent_id, name) " +
            "OVERRIDING SYSTEM VALUE VALUES ('tenant-a', ?, ?)",
            parentId, name);
        Long id = jdbc.queryForObject(
            "SELECT id FROM catalog_node WHERE tenant_id = 'tenant-a' AND name = ?", Long.class, name);
        recomputeNodePath(id);
        // 2) 内存 store
        TenantContext.set("tenant-a");
        try {
            store.save(CatalogNode.folder(parentId, name, "/"));
        } finally {
            TenantContext.clear();
        }
        return id;
    }

    private Long seedUserAndOrg(String username, long orgId) {
        // 1) 用户
        jdbc.update("INSERT INTO app_user (tenant_id, username) VALUES ('tenant-a', ?)", username);
        Long userId = jdbc.queryForObject(
            "SELECT id FROM app_user WHERE tenant_id = ? AND username = ?", Long.class,
            "tenant-a", username);
        // 2) 组织 (id 显式插入)
        jdbc.update("INSERT INTO org_unit (tenant_id, id, name) " +
                    "OVERRIDING SYSTEM VALUE VALUES ('tenant-a', ?, ?)", orgId, "org-" + orgId);
        recomputeOrgPath(orgId);
        // 3) 用户∈组织
        jdbc.update("INSERT INTO user_org (tenant_id, user_id, org_unit_id) VALUES ('tenant-a', ?, ?)",
            userId, orgId);
        return userId;
    }

    private void recomputeNodePath(long id) {
        String path = "/" + id + "/";
        Long pid = jdbc.queryForObject(
            "SELECT parent_id FROM catalog_node WHERE id = ?", Long.class, id);
        if (pid != null) {
            String parentPath = jdbc.queryForObject(
                "SELECT path FROM catalog_node WHERE id = ?", String.class, pid);
            path = parentPath + id + "/";
        }
        jdbc.update("UPDATE catalog_node SET path = ? WHERE id = ?", path, id);
    }

    private void recomputeOrgPath(long id) {
        String path = "/" + id + "/";
        Long pid = jdbc.queryForObject(
            "SELECT parent_id FROM org_unit WHERE id = ?", Long.class, id);
        if (pid != null) {
            String parentPath = jdbc.queryForObject(
                "SELECT path FROM org_unit WHERE id = ?", String.class, pid);
            path = parentPath + id + "/";
        }
        jdbc.update("UPDATE org_unit SET path = ? WHERE id = ?", path, id);
    }
}
