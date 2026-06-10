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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Testcontainers
class EditLockAndVersionIT extends IntegrationTest {

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:RELEASE.2024-12-18T13-15-44Z");

    @DynamicPropertySource
    static void registerMinio(DynamicPropertyRegistry r) {
        r.add("kep.storage.endpoint", minio::getS3URL);
        r.add("kep.storage.access-key", minio::getUserName);
        r.add("kep.storage.secret-key", minio::getPassword);
        r.add("kep.storage.bucket", () -> "kep-it-b1");
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
    void clearContexts() {
        TenantContext.clear();
        com.kep.shared.security.SecurityContext.clear();
    }

    @Test
    void acquire_lock_succeeds_when_free() throws Exception {
        long userId = seedBaseAndUser();
        seedKnowledge(userId);

        mvc.perform(post("/api/knowledge/1/lock")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.holderId").value(userId))
           .andExpect(jsonPath("$.data.knowledgeId").value(1));
    }

    @Test
    void acquire_lock_returns_409_when_held_by_other() throws Exception {
        long userA = seedBaseAndUser();
        long userB = seedUserB();
        seedKnowledge(userA);

        mvc.perform(post("/api/knowledge/1/lock")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userA)))
           .andExpect(status().isOk());

        mvc.perform(post("/api/knowledge/1/lock")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userB)))
           .andExpect(status().isConflict());
    }

    @Test
    void release_lock_by_holder_succeeds() throws Exception {
        long userId = seedBaseAndUser();
        seedKnowledge(userId);

        mvc.perform(post("/api/knowledge/1/lock")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk());

        mvc.perform(delete("/api/knowledge/1/lock")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk());

        // 再次获取成功
        mvc.perform(post("/api/knowledge/1/lock")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk());
    }

    @Test
    void release_lock_by_non_holder_returns_403() throws Exception {
        long userA = seedBaseAndUser();
        long userB = seedUserB();
        seedKnowledge(userA);

        mvc.perform(post("/api/knowledge/1/lock")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userA)))
           .andExpect(status().isOk());

        mvc.perform(delete("/api/knowledge/1/lock")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userB)))
           .andExpect(status().isForbidden());
    }

    @Test
    void create_v2_with_lock_succeeds() throws Exception {
        long userId = seedBaseAndUser();
        seedKnowledge(userId);

        mvc.perform(post("/api/knowledge/1/lock")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk());

        byte[] docx = new ClassPathResource("catalog/fixtures/test-doc.docx").getInputStream().readAllBytes();
        mvc.perform(multipart("/api/knowledge/1/versions")
                .file(new MockMultipartFile("file", "v2.docx", DOCX_CT, docx))
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.currentVersion.versionNo").value(2));
    }

    @Test
    void create_v2_without_lock_returns_403() throws Exception {
        long userId = seedBaseAndUser();
        seedKnowledge(userId);
        // 不抢锁

        byte[] docx = new ClassPathResource("catalog/fixtures/test-doc.docx").getInputStream().readAllBytes();
        mvc.perform(multipart("/api/knowledge/1/versions")
                .file(new MockMultipartFile("file", "v2.docx", DOCX_CT, docx))
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isForbidden());
    }

    @Test
    void put_html_content_returns_501() throws Exception {
        long userId = seedBaseAndUser();
        seedKnowledge(userId);

        mvc.perform(put("/api/knowledge/1/content")
                .contentType(MediaType.TEXT_HTML)
                .content("<html><body>v2 html</body></html>")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isNotImplemented());
    }

    // ---------- helpers ----------

    /** 灌 catalog_node id=1, org_unit id=1, app_user (admin-style), ACL READ+WRITE, 返回 user id。 */
    private long seedBaseAndUser() {
        jdbc.update("INSERT INTO catalog_node (tenant_id, name) " +
                    "OVERRIDING SYSTEM VALUE VALUES (?, 'root')", TENANT);
        jdbc.update("UPDATE catalog_node SET path = '/1/' WHERE id = 1");

        jdbc.update("INSERT INTO org_unit (tenant_id, name) " +
                    "OVERRIDING SYSTEM VALUE VALUES (?, 'org-root')", TENANT);
        jdbc.update("UPDATE org_unit SET path = '/1/' WHERE id = 1");

        String username = "user-" + System.nanoTime();
        jdbc.update("INSERT INTO app_user (tenant_id, username) VALUES (?, ?)", TENANT, username);
        long userId = jdbc.queryForObject(
            "SELECT id FROM app_user WHERE tenant_id = ? AND username = ?",
            Long.class, TENANT, username);
        jdbc.update("INSERT INTO user_org (tenant_id, user_id, org_unit_id) VALUES (?, ?, ?)",
            TENANT, userId, 1L);

        jdbc.update(
            "INSERT INTO acl (tenant_id, resource_type, resource_id, subject_type, subject_id, permission, effect) " +
            "VALUES (?, 'CATALOG_NODE', 1, 'USER', ?, 'READ', 'GRANT')", TENANT, userId);
        jdbc.update(
            "INSERT INTO acl (tenant_id, resource_type, resource_id, subject_type, subject_id, permission, effect) " +
            "VALUES (?, 'CATALOG_NODE', 1, 'USER', ?, 'WRITE', 'GRANT')", TENANT, userId);

        return userId;
    }

    private long seedUserB() {
        String username = "userB-" + System.nanoTime();
        jdbc.update("INSERT INTO app_user (tenant_id, username) VALUES (?, ?)", TENANT, username);
        long userId = jdbc.queryForObject(
            "SELECT id FROM app_user WHERE tenant_id = ? AND username = ?",
            Long.class, TENANT, username);
        jdbc.update("INSERT INTO user_org (tenant_id, user_id, org_unit_id) VALUES (?, ?, ?)",
            TENANT, userId, 1L);
        jdbc.update(
            "INSERT INTO acl (tenant_id, resource_type, resource_id, subject_type, subject_id, permission, effect) " +
            "VALUES (?, 'CATALOG_NODE', 1, 'USER', ?, 'WRITE', 'GRANT')", TENANT, userId);
        return userId;
    }

    /** 上传 v1，灌一份 knowledge。 */
    private void seedKnowledge(long userId) throws Exception {
        byte[] docx = new ClassPathResource("catalog/fixtures/test-doc.docx").getInputStream().readAllBytes();
        mvc.perform(multipart("/api/knowledge")
                .file(new MockMultipartFile("file", "v1.docx", DOCX_CT, docx))
                .param("catalogNodeId", "1")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk());
    }
}
