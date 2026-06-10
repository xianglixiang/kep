package com.kep.document;

import com.kep.shared.security.SecurityFilter;
import com.kep.shared.tenant.TenantContext;
import com.kep.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Testcontainers
class DocumentServiceIT extends IntegrationTest {

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

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE TABLE acl, knowledge_version, knowledge, " +
                     "user_org, org_unit, catalog_node, app_user, review_request RESTART IDENTITY CASCADE");
    }

    @AfterEach
    void clearContexts() {
        TenantContext.clear();
        com.kep.shared.security.SecurityContext.clear();
    }

    @Test
    void upload_creates_knowledge_and_v1() throws Exception {
        long userId = seedBaseAndUser();
        // 补 WRITE：seedBaseAndUser 只灌 READ,WRITE 由各测试按需加
        seedAcl(1L, userId, "WRITE", "GRANT");

        byte[] docx = new ClassPathResource("catalog/fixtures/test-doc.docx").getInputStream().readAllBytes();
        MockMultipartFile file = new MockMultipartFile("file", "x.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docx);

        mvc.perform(multipart("/api/knowledge")
                .file(file)
                .param("catalogNodeId", "1")
                .param("title", "测试上传")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.id").value(1))
           .andExpect(jsonPath("$.data.title").value("测试上传"))
           .andExpect(jsonPath("$.data.currentVersion.versionNo").value(1))
           .andExpect(jsonPath("$.data.currentVersion.contentRichtext").exists());
    }

    @Test
    void upload_denied_when_no_write_acl() throws Exception {
        long userId = seedBaseAndUser();
        // seedBaseAndUser 只配了 READ,故意不再配 WRITE → 期望 403

        byte[] docx = new ClassPathResource("catalog/fixtures/test-doc.docx").getInputStream().readAllBytes();
        MockMultipartFile file = new MockMultipartFile("file", "x.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docx);

        mvc.perform(multipart("/api/knowledge")
                .file(file)
                .param("catalogNodeId", "1")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isForbidden());
    }

    @Test
    void get_file_returns_original_bytes() throws Exception {
        long userId = seedBaseAndUser();
        seedAcl(1L, userId, "WRITE", "GRANT");

        byte[] docx = new ClassPathResource("catalog/fixtures/test-doc.docx").getInputStream().readAllBytes();
        MockMultipartFile file = new MockMultipartFile("file", "x.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docx);

        mvc.perform(multipart("/api/knowledge")
                .file(file)
                .param("catalogNodeId", "1")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk());

        mvc.perform(get("/api/knowledge/1/file")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk())
           .andExpect(content().bytes(docx));
    }

    @Test
    void cross_tenant_returns_404() throws Exception {
        // 注意:Knowledge 继承 TenantAwareEntity(@TenantId),Hibernate 在 SELECT 时
        // 自动追加 tenant_id 过滤,跨租户 findById(1) 直接返回空 → 404。
        // 这与 CatalogService 的 403 行为不同(后者在 PermissionChecker 之前没
        // 实体级租户过滤,先过 ACL 拒绝)。两条路都"安全",仅入口语义不同。
        long userId = seedBaseAndUser();
        seedAcl(1L, userId, "WRITE", "GRANT");

        byte[] docx = new ClassPathResource("catalog/fixtures/test-doc.docx").getInputStream().readAllBytes();
        MockMultipartFile file = new MockMultipartFile("file", "x.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docx);

        // 上传成功
        mvc.perform(multipart("/api/knowledge")
                .file(file)
                .param("catalogNodeId", "1")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk());

        // 换 tenant 头:被 @TenantId 拦截,直接 404
        mvc.perform(get("/api/knowledge/1")
                .header(SecurityFilter.TENANT_HEADER, "tenant-b")
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isNotFound());
    }

    // ---------- 灌数据 ----------

    /**
     * 在 tenant-a 下灌 catalog_node (id=1)、一个 user、加入 org 1，配置 READ。
     * 注意:故意只配 READ,WRITE 由各测试按需 seedAcl(...,"WRITE","GRANT") 加,
     * 这样 upload_denied_when_no_write_acl 不需要先删除 WRITE。
     * 返回该 user 的 id。
     */
    private long seedBaseAndUser() {
        // 1. catalog_node id=1, path=/1/
        jdbc.update("INSERT INTO catalog_node (tenant_id, name) " +
                    "OVERRIDING SYSTEM VALUE VALUES (?, 'root')", TENANT);
        jdbc.update("UPDATE catalog_node SET path = '/1/' WHERE id = 1");

        // 2. app_user
        String username = "user-" + System.nanoTime();
        jdbc.update("INSERT INTO app_user (tenant_id, username) VALUES (?, ?)", TENANT, username);
        long userId = jdbc.queryForObject(
            "SELECT id FROM app_user WHERE tenant_id = ? AND username = ?",
            Long.class, TENANT, username);

        // 3. org_unit id=1
        jdbc.update("INSERT INTO org_unit (tenant_id, name) " +
                    "OVERRIDING SYSTEM VALUE VALUES (?, 'org-root')", TENANT);
        jdbc.update("UPDATE org_unit SET path = '/1/' WHERE id = 1");

        // 4. user_org
        jdbc.update("INSERT INTO user_org (tenant_id, user_id, org_unit_id) VALUES (?, ?, ?)",
            TENANT, userId, 1L);

        // 5. ACL READ (WRITE 由调用方按需补)
        seedAcl(1L, userId, "READ", "GRANT");

        return userId;
    }

    private void seedAcl(long resourceId, long userId, String permission, String effect) {
        jdbc.update(
            "INSERT INTO acl (tenant_id, resource_type, resource_id, subject_type, subject_id, permission, effect) " +
            "VALUES (?, 'CATALOG_NODE', ?, 'USER', ?, ?, ?)",
            TENANT, resourceId, userId, permission, effect);
    }
}
