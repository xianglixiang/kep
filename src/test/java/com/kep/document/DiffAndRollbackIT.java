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
class DiffAndRollbackIT extends IntegrationTest {

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:RELEASE.2024-12-18T13-15-44Z");

    @DynamicPropertySource
    static void registerMinio(DynamicPropertyRegistry r) {
        r.add("kep.storage.endpoint", minio::getS3URL);
        r.add("kep.storage.access-key", minio::getUserName);
        r.add("kep.storage.secret-key", minio::getPassword);
        r.add("kep.storage.bucket", () -> "kep-it-b2");
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

    // ---------- diff tests ----------

    @Test
    void diff_equal_versions_returns_400() throws Exception {
        long userId = seedBaseAndUser();
        uploadV1(userId);

        mvc.perform(get("/api/knowledge/1/diff?from=1&to=1")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isBadRequest());
    }

    @Test
    void diff_v1_v2_with_same_content_returns_equal_segment() throws Exception {
        long userId = seedBaseAndUser();
        uploadV1(userId);
        uploadV2(userId);  // 同 test-doc.docx，但作为 v2

        mvc.perform(get("/api/knowledge/1/diff?from=1&to=2")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.segments[0].type").value("equal"));
    }

    @Test
    void diff_returns_proper_json_shape() throws Exception {
        long userId = seedBaseAndUser();
        uploadV1(userId);
        uploadV2(userId);

        mvc.perform(get("/api/knowledge/1/diff?from=1&to=2")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.from.versionNo").value(1))
           .andExpect(jsonPath("$.data.to.versionNo").value(2))
           .andExpect(jsonPath("$.data.segments").isArray())
           .andExpect(jsonPath("$.data.segments[0].type").exists());
    }

    // ---------- rollback tests ----------

    @Test
    void rollback_creates_new_version_with_rollback_change_type() throws Exception {
        long userId = seedBaseAndUser();
        uploadV1(userId);
        uploadV2(userId);

        // 拿锁
        mvc.perform(post("/api/knowledge/1/lock")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk());

        // 回滚到 v1 → v3 (change_type=ROLLBACK)
        mvc.perform(post("/api/knowledge/1/rollback/1")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.currentVersion.versionNo").value(3))
           .andExpect(jsonPath("$.data.currentVersion.changeType").value("ROLLBACK"));
    }

    @Test
    void rollback_without_lock_returns_403() throws Exception {
        long userId = seedBaseAndUser();
        uploadV1(userId);
        // 不抢锁

        mvc.perform(post("/api/knowledge/1/rollback/1")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isForbidden());
    }

    @Test
    void rollback_to_nonexistent_version_returns_404() throws Exception {
        long userId = seedBaseAndUser();
        uploadV1(userId);
        mvc.perform(post("/api/knowledge/1/lock")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk());

        mvc.perform(post("/api/knowledge/1/rollback/99")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isNotFound());
    }

    // ---------- force 抢占 tests ----------

    @Test
    void force_acquire_takes_over_from_active_holder() throws Exception {
        long userA = seedBaseAndUser();
        long userB = seedUserB();
        uploadV1(userA);

        // userA 拿锁
        mvc.perform(post("/api/knowledge/1/lock")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userA)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.holderId").value(userA));

        // userB 不带 force → 409
        mvc.perform(post("/api/knowledge/1/lock")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userB)))
           .andExpect(status().isConflict());

        // userB 带 force=true → 200,holder=userB
        mvc.perform(post("/api/knowledge/1/lock?force=true")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userB)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.holderId").value(userB));
    }

    // ---------- helpers ----------

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

    private void uploadV1(long userId) throws Exception {
        byte[] docx = new ClassPathResource("catalog/fixtures/test-doc.docx").getInputStream().readAllBytes();
        mvc.perform(multipart("/api/knowledge")
                .file(new MockMultipartFile("file", "v1.docx", DOCX_CT, docx))
                .param("catalogNodeId", "1")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk());
    }

    private void uploadV2(long userId) throws Exception {
        // 先拿锁
        mvc.perform(post("/api/knowledge/1/lock")
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk());

        byte[] docx = new ClassPathResource("catalog/fixtures/test-doc.docx").getInputStream().readAllBytes();
        mvc.perform(multipart("/api/knowledge/1/versions")
                .file(new MockMultipartFile("file", "v2.docx", DOCX_CT, docx))
                .header(SecurityFilter.TENANT_HEADER, TENANT)
                .header(SecurityFilter.USER_HEADER, String.valueOf(userId)))
           .andExpect(status().isOk());
    }
}
