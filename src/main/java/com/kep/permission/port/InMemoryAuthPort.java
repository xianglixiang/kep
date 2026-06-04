package com.kep.permission.port;

import com.kep.permission.api.AclRow;
import com.kep.permission.api.Permission;
import com.kep.permission.api.Subject;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * local-mock 下的鉴权数据存储 + 三个端口合一实现。
 * 维护：user 直属 org、org 的 path、catalog_node 的 path、ACL 行。
 * 由 ThreeTierPermissionCheckerTest 与 CatalogPermissionIT 共用同一份内存视图。
 */
@Component
@Profile("local-mock")
public class InMemoryAuthPort implements UserOrgPort, CatalogPathPort, AclPort {

    // userId -> 该 user 直属 orgUnitId 集合
    private final Map<Long, Set<Long>> userDirectOrgs = new ConcurrentHashMap<>();
    // orgUnitId -> path
    private final Map<Long, String> orgPaths = new ConcurrentHashMap<>();
    // catalogNodeId -> path
    private final Map<Long, String> nodePaths = new ConcurrentHashMap<>();
    // ACL 行（按租户分区）
    private final Map<String, List<AclRow>> acls = new ConcurrentHashMap<>();

    // ========== 数据灌入（测试/引导用） ==========

    public void addUserDirectOrg(long userId, long orgId) {
        userDirectOrgs.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(orgId);
    }

    public void addOrg(long orgId, String path) {
        orgPaths.put(orgId, path);
    }

    public void addCatalogNode(long nodeId, String path) {
        nodePaths.put(nodeId, path);
    }

    public void addAcl(String tenantId, AclRow row) {
        acls.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>()).add(row);
    }

    public void clear() {
        userDirectOrgs.clear();
        orgPaths.clear();
        nodePaths.clear();
        acls.clear();
    }

    // ========== 端口实现 ==========

    @Override
    public Set<Long> ancestorOrgIds(long userId) {
        Set<Long> direct = userDirectOrgs.getOrDefault(userId, Set.of());
        if (direct.isEmpty()) return Set.of();
        Set<Long> result = new HashSet<>();
        for (Long orgId : direct) {
            collectAncestors(orgId, result);
        }
        return result;
    }

    private void collectAncestors(long orgId, Set<Long> acc) {
        if (!acc.add(orgId)) return;
        String path = orgPaths.get(orgId);
        if (path == null) return;
        String trimmed = path.replaceAll("^/|/$", "");
        if (trimmed.isEmpty()) return;
        String[] parts = trimmed.split("/");
        if (parts.length < 2) return;
        try {
            // 父 org id = 倒数第二个段（自身是最后一段）
            long parentId = Long.parseLong(parts[parts.length - 2]);
            if (orgPaths.containsKey(parentId)) {
                collectAncestors(parentId, acc);
            }
        } catch (NumberFormatException ignore) {}
    }

    @Override
    public List<Long> ancestorIds(long catalogNodeId) {
        String path = nodePaths.get(catalogNodeId);
        if (path == null) return List.of();
        String trimmed = path.replaceAll("^/|/$", "");
        if (trimmed.isEmpty()) return List.of();
        List<Long> ids = new ArrayList<>();
        for (String s : trimmed.split("/")) {
            try { ids.add(Long.parseLong(s)); } catch (NumberFormatException ignore) {}
        }
        return ids;
    }

    @Override
    public List<AclRow> findHits(String tenantId, Collection<Long> resourceIds,
                                 Set<Subject> subjects, Permission permission) {
        List<AclRow> all = acls.getOrDefault(tenantId, List.of());
        List<AclRow> result = new ArrayList<>();
        for (AclRow r : all) {
            if (!resourceIds.contains(r.resourceId())) continue;
            if (!r.permission().equals(permission.name())) continue;
            for (Subject s : subjects) {
                if (s.type().equals(r.subject().type()) && s.id() == r.subject().id()) {
                    result.add(r);
                    break;
                }
            }
        }
        return result;
    }
}
