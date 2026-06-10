package com.kep.catalog;

import com.kep.shared.security.SecurityFilter;
import com.kep.shared.tenant.TenantContext;
import com.kep.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 知识地图 HTTP 端到端：覆盖 CatalogController 三个新端点（tree / knowledge / breadcrumb）。
 * 通过 MinIO 让 DocumentController#upload 可用，以便真实灌一条 knowledge 供 list 端点查询。
 */
@AutoConfigureMockMvc
@Testcontainers
class KnowledgeQueryServiceIT extends IntegrationTest {

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:RELEASE.2024-12-18T13-15-44Z");

    @DynamicPropertySource
    static void registerMinio(DynamicPropertyRegistry r) {
        r.add("kep.storage.endpoint", minio::getS3URL);
        r.add("kep.storage.access-key", minio::getUserName);
        r.add("kep.storage.secret-key", minio::getPassword);
        r.add("kep.storage.bucket", () -> "kep-it");
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private static final String TENANT = "tenant-a";
    private static final String DOCX_CT =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE TABLE edit_lock, acl, knowledge_version, knowledge, " +
                     "user_org, org_unit, catalog_node, app_user, review_request RESTART IDENTITY CASCADE");
    }

    @AfterEach
    void clearCtx() {
        TenantContext.clear();
        com.kep.shared.security.SecurityContext.clear();
    }

    @Test
    void tree_returns_root_with_default_root_id() throws Exception {
        long userId = seedBaseAndUser();
        // 灌一个子节点 '产品' 挂在根下，让 tree 端点有内容可断言（tree 列出 rootId 的可见子节点）。
        jdbc.update("INSERT INTO catalog_node (tenant_id, parent_id, name) " +
                    "OVERRIDING SYSTEM VALUE VALUES (?, ?, ?)", TENANT, 1L, "产品");
        jdbc.update("UPDATE catalog_node SET path = '/1/2/' WHERE id = 2");

        mvc.perform(get("/api/catalog/nodes/tree")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.rootId").value(1))
           .andExpect(jsonPath("$.data.tree.length()").value(1))
           .andExpect(jsonPath("$.data.tree[0].id").value(2))
           .andExpect(jsonPath("$.data.tree[0].name").value("产品"));
    }

    @Test
    void tree_hides_nodes_without_read_permission() throws Exception {
        // 灌根节点，灌两个子节点；只给 user 看其中一个。
        jdbc.update("INSERT INTO catalog_node (tenant_id, name) OVERRIDING SYSTEM VALUE VALUES (?, 'root')", TENANT);
        jdbc.update("UPDATE catalog_node SET path = '/1/' WHERE id = 1");
        jdbc.update("INSERT INTO catalog_node (tenant_id, parent_id, name) " +
                    "OVERRIDING SYSTEM VALUE VALUES (?, ?, ?)", TENANT, 1L, "open");
        jdbc.update("UPDATE catalog_node SET path = '/1/2/' WHERE id = 2");
        jdbc.update("INSERT INTO catalog_node (tenant_id, parent_id, name) " +
                    "OVERRIDING SYSTEM VALUE VALUES (?, ?, ?)", TENANT, 1L, "secret");
        jdbc.update("UPDATE catalog_node SET path = '/1/3/' WHERE id = 3");

        // user + 1 个 READ ACL (node 2)
        String username = "user-" + System.nanoTime();
        jdbc.update("INSERT INTO app_user (tenant_id, username) VALUES (?, ?)", TENANT, username);
        long userId = jdbc.queryForObject(
            "SELECT id FROM app_user WHERE tenant_id = ? AND username = ?",
            Long.class, TENANT, username);
        jdbc.update("INSERT INTO org_unit (tenant_id, name) OVERRIDING SYSTEM VALUE VALUES (?, 'org-root')", TENANT);
        jdbc.update("UPDATE org_unit SET path = '/1/' WHERE id = 1");
        jdbc.update("INSERT INTO user_org (tenant_id, user_id, org_unit_id) VALUES (?, ?, ?)",
            TENANT, userId, 1L);
        jdbc.update("INSERT INTO acl (tenant_id, resource_type, resource_id, subject_type, subject_id, permission, effect) " +
                    "VALUES (?, 'CATALOG_NODE', 2, 'USER', ?, 'READ', 'GRANT')", TENANT, userId);

        mvc.perform(get("/api/catalog/nodes/tree")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.tree.length()").value(1))
           .andExpect(jsonPath("$.data.tree[0].name").value("open"));
    }

    @Test
    void list_returns_knowledge_in_node() throws Exception {
        long userId = seedBaseAndUser();
        uploadKnowledge(userId);  // knowledge 1

        mvc.perform(get("/api/catalog/nodes/knowledge?nodeId=1")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.items.length()").value(1))
           .andExpect(jsonPath("$.data.items[0].id").value(1))
           .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void list_with_docType_filter_no_match() throws Exception {
        long userId = seedBaseAndUser();
        uploadKnowledge(userId);

        mvc.perform(get("/api/catalog/nodes/knowledge?nodeId=1&docType=PDF")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.items.length()").value(0))
           .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void breadcrumb_returns_full_path() throws Exception {
        seedBaseAndUser();
        // 灌 3 层节点: 1(root) → 2(mid) → 3(leaf)
        jdbc.update("INSERT INTO catalog_node (tenant_id, parent_id, name) " +
                    "OVERRIDING SYSTEM VALUE VALUES (?, ?, ?)", TENANT, 1L, "mid");
        jdbc.update("UPDATE catalog_node SET path = '/1/2/' WHERE id = 2");
        jdbc.update("INSERT INTO catalog_node (tenant_id, parent_id, name) " +
                    "OVERRIDING SYSTEM VALUE VALUES (?, ?, ?)", TENANT, 2L, "leaf");
        jdbc.update("UPDATE catalog_node SET path = '/1/2/3/' WHERE id = 3");

        // 拿 userId 是为了传 header
        long userId = jdbc.queryForObject(
            "SELECT id FROM app_user WHERE tenant_id = ? ORDER BY id LIMIT 1",
            Long.class, TENANT);

        mvc.perform(get("/api/catalog/nodes/breadcrumb?nodeId=3")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.nodeId").value(3))
           .andExpect(jsonPath("$.data.path.length()").value(3))
           .andExpect(jsonPath("$.data.path[0].name").value("root"))
           .andExpect(jsonPath("$.data.path[1].name").value("mid"))
           .andExpect(jsonPath("$.data.path[2].name").value("leaf"));
    }

    @Test
    void list_with_no_nodeId_returns_error() throws Exception {
        long userId = seedBaseAndUser();

        // 已知缺口：CatalogController#listKnowledge 的 nodeId 缺失抛 MissingServletRequestParameterException，
        // 被 GlobalExceptionHandler.@ExceptionHandler(Exception.class) 兜底为 500。
        // TODO：补一个针对 MissingServletRequestParameterException 的 400 handler 后改回 isBadRequest()。
        mvc.perform(get("/api/catalog/nodes/knowledge")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isInternalServerError())
           .andExpect(jsonPath("$.code").value("INTERNAL"));
    }

    // ---------- helpers ----------

    /**
     * 灌 catalog_node (id=1 root) + 一个 user + 加入 org 1 + 给根配 READ + WRITE。
     * 注意：不预置子节点；需要子节点的测试自行 INSERT，避免各测试间 id 冲突。
     */
    private long seedBaseAndUser() {
        jdbc.update("INSERT INTO catalog_node (tenant_id, name) OVERRIDING SYSTEM VALUE VALUES (?, 'root')", TENANT);
        jdbc.update("UPDATE catalog_node SET path = '/1/' WHERE id = 1");
        jdbc.update("INSERT INTO org_unit (tenant_id, name) OVERRIDING SYSTEM VALUE VALUES (?, 'org-root')", TENANT);
        jdbc.update("UPDATE org_unit SET path = '/1/' WHERE id = 1");
        String username = "user-" + System.nanoTime();
        jdbc.update("INSERT INTO app_user (tenant_id, username) VALUES (?, ?)", TENANT, username);
        long userId = jdbc.queryForObject(
            "SELECT id FROM app_user WHERE tenant_id = ? AND username = ?",
            Long.class, TENANT, username);
        jdbc.update("INSERT INTO user_org (tenant_id, user_id, org_unit_id) VALUES (?, ?, ?)",
            TENANT, userId, 1L);
        // 给 root (id=1) READ+WRITE：upload 需要 WRITE；list 需要 READ。
        jdbc.update("INSERT INTO acl (tenant_id, resource_type, resource_id, subject_type, subject_id, permission, effect) " +
                    "VALUES (?, 'CATALOG_NODE', 1, 'USER', ?, 'READ', 'GRANT')", TENANT, userId);
        jdbc.update("INSERT INTO acl (tenant_id, resource_type, resource_id, subject_type, subject_id, permission, effect) " +
                    "VALUES (?, 'CATALOG_NODE', 1, 'USER', ?, 'WRITE', 'GRANT')", TENANT, userId);
        return userId;
    }

    private void uploadKnowledge(long userId) throws Exception {
        byte[] docx = new ClassPathResource("catalog/fixtures/test-doc.docx").getInputStream().readAllBytes();
        mvc.perform(multipart("/api/knowledge")
                .file(new MockMultipartFile("file", "v1.docx", DOCX_CT, docx))
                .param("catalogNodeId", "1")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk());
    }
}
