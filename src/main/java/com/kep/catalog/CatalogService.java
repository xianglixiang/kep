package com.kep.catalog;

import com.kep.catalog.dto.CreateNodeRequest;
import com.kep.catalog.dto.NodeView;
import com.kep.permission.api.Permission;
import com.kep.permission.api.PermissionChecker;
import com.kep.shared.error.BusinessException;
import com.kep.shared.error.ErrorCode;
import com.kep.shared.tenant.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogService {

    private final CatalogNodeStore store;
    private final PermissionChecker permissionChecker;

    public CatalogService(CatalogNodeStore store, PermissionChecker permissionChecker) {
        this.store = store;
        this.permissionChecker = permissionChecker;
    }

    @Transactional
    public NodeView create(Long userId, CreateNodeRequest req) {
        if (req.parentId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "parentId 必须非空；根目录由 bootstrapRoot 预置");
        }
        permissionChecker.check(userId, req.parentId(), Permission.WRITE);
        CatalogNode saved = store.save(CatalogNode.folder(req.parentId(), req.name(), "/"));
        return NodeView.from(saved);
    }

    @Transactional(readOnly = true)
    public List<NodeView> listChildren(Long userId, Long parentId) {
        permissionChecker.check(userId, parentId, Permission.READ);
        List<CatalogNode> children = store.findByParent(parentId);
        // 过滤掉 user 不可 READ 的子节点（子节点可能有自己的 DENY ACL）
        return children.stream()
            .filter(c -> {
                try {
                    permissionChecker.check(userId, c.getId(), Permission.READ);
                    return true;
                } catch (AccessDeniedException e) {
                    return false;
                }
            })
            .map(NodeView::from)
            .toList();
    }

    /** 系统级：引导脚本调用，在某租户下创建根 catalog_node（path=/<id>/）。 */
    @Transactional
    public NodeView bootstrapRoot(String tenantId) {
        String prev = TenantContext.get();
        try {
            TenantContext.set(tenantId);
            CatalogNode root = CatalogNode.folder(null, "root", "/");
            store.save(root);
            return NodeView.from(root);
        } finally {
            if (prev != null) TenantContext.set(prev);
            else TenantContext.clear();
        }
    }
}
