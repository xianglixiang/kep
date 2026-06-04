package com.kep.permission;

import com.kep.permission.api.Permission;
import com.kep.permission.port.InMemoryAuthPort;
import com.kep.shared.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.access.AccessDeniedException;

import java.util.stream.Stream;

import static com.kep.permission.AuthFixtures.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThreeTierPermissionCheckerTest {

    private static final InMemoryAuthPort port = new InMemoryAuthPort();
    private final ThreeTierPermissionChecker checker = new ThreeTierPermissionChecker(port, port, port);

    @AfterEach
    void clear() { port.clear(); TenantContext.clear(); }

    // 用例数据：(场景名, fixture 动作, 请求, 预期通过/拒绝)
    static Stream<Arguments> cases() {
        return Stream.of(
            case1_noAcl_denied(),
            case2_directOrg_grant_pass(),
            case3_ancestorOrg_grant_pass(),
            case4_childDeny_overridesParentGrant(),
            case5_denyWins_sameResource(),
            case6_writeImpliesRead(),
            case7_readDoesNotImplyWrite(),
            case8_crossTenant_denied(),
            case9_denyWins_sameSubject(),
            case10_personalDeny_overridesOrgGrant(),
            case11_leafDeny_alone_denied(),
            case12_anyDeny_denies()
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void table_driven(String name, Runnable setup, long userId, long nodeId,
                      Permission perm, boolean expectPass) {
        setup.run();
        if (expectPass) {
            checker.check(userId, nodeId, perm);
        } else {
            assertThatThrownBy(() -> checker.check(userId, nodeId, perm))
                .isInstanceOf(AccessDeniedException.class);
        }
    }

    // ---------- 用例工厂 ----------

    private static Arguments case1_noAcl_denied() {
        return Arguments.of("no_acl_denied", (Runnable) () -> {
            TenantContext.set(TENANT_A);
            seedOrgTree(port);
            seedUser(port, 100L, 4L);
            seedNode(port, 1L, "/1/");
        }, 100L, 1L, Permission.READ, false);
    }

    private static Arguments case2_directOrg_grant_pass() {
        return Arguments.of("direct_org_grant_pass", (Runnable) () -> {
            TenantContext.set(TENANT_A);
            seedOrgTree(port);
            seedUser(port, 100L, 4L);
            seedNode(port, 1L, "/1/");
            seedAcl(port, TENANT_A, 1L, org(4L), Permission.READ, "GRANT");
        }, 100L, 1L, Permission.READ, true);
    }

    private static Arguments case3_ancestorOrg_grant_pass() {
        return Arguments.of("ancestor_org_grant_pass", (Runnable) () -> {
            TenantContext.set(TENANT_A);
            seedOrgTree(port);
            seedUser(port, 100L, 4L);
            seedNode(port, 1L, "/1/");
            seedNode(port, 4L, "/1/4/");
            seedAcl(port, TENANT_A, 1L, org(1L), Permission.READ, "GRANT");
        }, 100L, 4L, Permission.READ, true);
    }

    private static Arguments case4_childDeny_overridesParentGrant() {
        return Arguments.of("child_deny_overrides_parent_grant", (Runnable) () -> {
            TenantContext.set(TENANT_A);
            seedOrgTree(port);
            seedUser(port, 100L, 4L);
            seedNode(port, 1L, "/1/");
            seedNode(port, 4L, "/1/4/");
            seedAcl(port, TENANT_A, 1L, org(1L), Permission.READ, "GRANT");
            seedAcl(port, TENANT_A, 4L, user(100L), Permission.READ, "DENY");
        }, 100L, 4L, Permission.READ, false);
    }

    private static Arguments case5_denyWins_sameResource() {
        return Arguments.of("deny_wins_same_resource", (Runnable) () -> {
            TenantContext.set(TENANT_A);
            seedOrgTree(port);
            seedUser(port, 100L, 4L);
            seedNode(port, 1L, "/1/");
            seedAcl(port, TENANT_A, 1L, org(1L), Permission.READ, "GRANT");
            seedAcl(port, TENANT_A, 1L, org(1L), Permission.READ, "DENY");
        }, 100L, 1L, Permission.READ, false);
    }

    private static Arguments case6_writeImpliesRead() {
        return Arguments.of("write_implies_read", (Runnable) () -> {
            TenantContext.set(TENANT_A);
            seedOrgTree(port);
            seedUser(port, 100L, 4L);
            seedNode(port, 1L, "/1/");
            seedAcl(port, TENANT_A, 1L, org(4L), Permission.WRITE, "GRANT");
        }, 100L, 1L, Permission.READ, true);
    }

    private static Arguments case7_readDoesNotImplyWrite() {
        return Arguments.of("read_does_not_imply_write", (Runnable) () -> {
            TenantContext.set(TENANT_A);
            seedOrgTree(port);
            seedUser(port, 100L, 4L);
            seedNode(port, 1L, "/1/");
            seedAcl(port, TENANT_A, 1L, org(4L), Permission.READ, "GRANT");
        }, 100L, 1L, Permission.WRITE, false);
    }

    private static Arguments case8_crossTenant_denied() {
        return Arguments.of("cross_tenant_denied", (Runnable) () -> {
            TenantContext.set(TENANT_A);
            seedOrgTree(port);
            seedUser(port, 100L, 4L);
            seedNode(port, 1L, "/1/");
            seedAcl(port, TENANT_B, 1L, org(4L), Permission.READ, "GRANT");
        }, 100L, 1L, Permission.READ, false);
    }

    private static Arguments case9_denyWins_sameSubject() {
        return Arguments.of("deny_wins_same_subject", (Runnable) () -> {
            TenantContext.set(TENANT_A);
            seedOrgTree(port);
            seedUser(port, 100L, 4L);
            seedNode(port, 1L, "/1/");
            seedAcl(port, TENANT_A, 1L, org(4L), Permission.WRITE, "GRANT");
            seedAcl(port, TENANT_A, 1L, org(4L), Permission.WRITE, "DENY");
        }, 100L, 1L, Permission.WRITE, false);
    }

    private static Arguments case10_personalDeny_overridesOrgGrant() {
        return Arguments.of("personal_deny_overrides_org_grant", (Runnable) () -> {
            TenantContext.set(TENANT_A);
            seedOrgTree(port);
            seedUser(port, 100L, 4L);
            seedNode(port, 1L, "/1/");
            seedNode(port, 4L, "/1/4/");
            seedAcl(port, TENANT_A, 4L, org(4L), Permission.READ, "GRANT");
            seedAcl(port, TENANT_A, 4L, user(100L), Permission.READ, "DENY");
        }, 100L, 4L, Permission.READ, false);
    }

    private static Arguments case11_leafDeny_alone_denied() {
        return Arguments.of("leaf_deny_alone_denied", (Runnable) () -> {
            TenantContext.set(TENANT_A);
            seedOrgTree(port);
            seedUser(port, 100L, 4L);
            seedNode(port, 1L, "/1/");
            seedNode(port, 4L, "/1/4/");
            seedNode(port, 9L, "/1/4/9/");
            seedAcl(port, TENANT_A, 9L, user(100L), Permission.READ, "DENY");
        }, 100L, 9L, Permission.READ, false);
    }

    private static Arguments case12_anyDeny_denies() {
        return Arguments.of("any_deny_denies", (Runnable) () -> {
            TenantContext.set(TENANT_A);
            seedOrgTree(port);
            seedUser(port, 100L, 4L, 5L);
            seedNode(port, 1L, "/1/");
            seedNode(port, 4L, "/1/4/");
            seedNode(port, 5L, "/1/5/");
            seedAcl(port, TENANT_A, 1L, org(4L), Permission.READ, "GRANT");
            seedAcl(port, TENANT_A, 1L, org(5L), Permission.READ, "DENY");
        }, 100L, 1L, Permission.READ, false);
    }
}
