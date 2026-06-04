package com.kep.permission;

import com.kep.permission.api.AclRow;
import com.kep.permission.api.Permission;
import com.kep.permission.api.PermissionChecker;
import com.kep.permission.api.Subject;
import com.kep.permission.port.AclPort;
import com.kep.permission.port.CatalogPathPort;
import com.kep.permission.port.UserOrgPort;
import com.kep.shared.tenant.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 三层鉴权算法：
 *   ① 租户隔离（TenantContext 必须非空）
 *   ② 目录 ACL 就近优先 + DENY 覆盖 GRANT
 *   ③ 读写分离（调用方传 Permission，SQL WHERE 吸收；WRITE 蕴含 READ 在调用方处理）
 *
 * 默认拒绝：任何路径无命中即拒绝。
 */
@Service
public class ThreeTierPermissionChecker implements PermissionChecker {

    private final UserOrgPort userOrgPort;
    private final CatalogPathPort catalogPathPort;
    private final AclPort aclPort;

    public ThreeTierPermissionChecker(UserOrgPort userOrgPort,
                                      CatalogPathPort catalogPathPort,
                                      AclPort aclPort) {
        this.userOrgPort = userOrgPort;
        this.catalogPathPort = catalogPathPort;
        this.aclPort = aclPort;
    }

    @Override
    public void check(Long userId, Long catalogNodeId, Permission required) {
        if (!has(userId, catalogNodeId, required)) {
            throw new AccessDeniedException(
                "无权访问: userId=" + userId + ", nodeId=" + catalogNodeId + ", perm=" + required);
        }
    }

    @Override
    public boolean has(Long userId, Long catalogNodeId, Permission required) {
        String tenant = TenantContext.get();
        if (tenant == null) return false;       // ① 租户隔离

        Set<Subject> subjects = new HashSet<>();
        subjects.add(Subject.user(userId));
        subjects.addAll(ancestorOrgsAsSubjects(userId));   // ② 主体解析

        List<Long> ancestorIds = catalogPathPort.ancestorIds(catalogNodeId);
        if (ancestorIds.isEmpty()) return false;

        // WRITE 蕴含 READ：请求 READ 时，WRITE 的 ACL 行同样视为命中。
        List<AclRow> hits = new ArrayList<>(aclPort.findHits(tenant, ancestorIds, subjects, required));
        if (required == Permission.READ) {
            hits.addAll(aclPort.findHits(tenant, ancestorIds, subjects, Permission.WRITE));
        }

        // 就近优先：对每个 (subject, permission) 取最近祖先的 effect
        // ancestorIds 顺序：i=0 最远祖先，i=末尾 自身（最近）。
        // 循环按 i 升序遍历，用 put 覆盖——靠后的（i 大的，更近）会覆盖前面的。
        Map<String, String> nearestEffect = new HashMap<>();
        for (int i = 0; i < ancestorIds.size(); i++) {
            long resourceId = ancestorIds.get(i);
            for (AclRow row : hits) {
                if (row.resourceId() != resourceId) continue;
                String key = row.subject().type() + ":" + row.subject().id();
                nearestEffect.put(key, row.effect());   // 后写覆盖前写（i 大的赢）
            }
        }
        boolean anyGrant = false;
        for (String e : nearestEffect.values()) {
            if ("DENY".equals(e)) return false;
            if ("GRANT".equals(e)) anyGrant = true;
        }
        return anyGrant;   // 默认拒绝
    }

    private Set<Subject> ancestorOrgsAsSubjects(long userId) {
        Set<Subject> set = new HashSet<>();
        for (Long ouId : userOrgPort.ancestorOrgIds(userId)) {
            set.add(Subject.orgUnit(ouId));
        }
        return set;
    }
}
