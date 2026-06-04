package com.kep.permission;

import com.kep.permission.api.AclRow;
import com.kep.permission.api.Permission;
import com.kep.permission.api.Subject;
import com.kep.permission.port.InMemoryAuthPort;

/** 测试 fixture 构造器——为 ThreeTierPermissionCheckerTest 与 CatalogPermissionIT 共享。 */
public final class AuthFixtures {

    public static final String TENANT_A = "tenant-a";
    public static final String TENANT_B = "tenant-b";

    /** 标准小型组织树：产品部(1) → 子组1(4) / 子组2(5) */
    public static void seedOrgTree(InMemoryAuthPort port) {
        port.addOrg(1L, "/1/");
        port.addOrg(4L, "/1/4/");
        port.addOrg(5L, "/1/5/");
    }

    public static void seedUser(InMemoryAuthPort port, long userId, long... directOrgIds) {
        for (long ou : directOrgIds) port.addUserDirectOrg(userId, ou);
    }

    public static void seedNode(InMemoryAuthPort port, long nodeId, String path) {
        port.addCatalogNode(nodeId, path);
    }

    public static void seedAcl(InMemoryAuthPort port, String tenant,
                               long resourceId, Subject subject, Permission perm, String effect) {
        port.addAcl(tenant, new AclRow(resourceId, subject, perm.name(), effect));
    }

    public static Subject user(long id) { return Subject.user(id); }
    public static Subject org(long id) { return Subject.orgUnit(id); }

    private AuthFixtures() {}
}
