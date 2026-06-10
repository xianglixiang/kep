package com.kep.catalog;

import com.kep.catalog.dto.BreadcrumbView;
import com.kep.catalog.dto.CatalogTreeNode;
import com.kep.catalog.dto.CatalogTreeView;
import com.kep.catalog.dto.KnowledgeListItem;
import com.kep.document.api.KnowledgeQueryApi;
import com.kep.document.api.KnowledgeView;
import com.kep.permission.api.Permission;
import com.kep.permission.api.PermissionChecker;
import com.kep.shared.error.BusinessException;
import com.kep.shared.error.ErrorCode;
import com.kep.shared.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class KnowledgeQueryService {

    private final KnowledgeQueryApi knowledgeApi;
    private final CatalogNodeStore nodeStore;
    private final PermissionChecker permissionChecker;

    public KnowledgeQueryService(KnowledgeQueryApi knowledgeApi,
                                 CatalogNodeStore nodeStore,
                                 PermissionChecker permissionChecker) {
        this.knowledgeApi = knowledgeApi;
        this.nodeStore = nodeStore;
        this.permissionChecker = permissionChecker;
    }

    /**
     * 目录树: 返回嵌套 JSON, user 看不到无 READ 权限的节点(子节点若无可见也不显示)。
     * 策略: 1) 全扫所有节点 2) 一次性 PermissionChecker.hasRead 收集 user 有权限的节点
     *        3) 内存组树 4) 递归裁剪。
     */
    @Transactional(readOnly = true)
    public CatalogTreeView tree(long userId, Long rootId) {
        if (rootId == null) rootId = 1L;
        List<CatalogNode> allNodes = nodeStore.findAll();
        Set<Long> readSet = new HashSet<>();
        for (CatalogNode n : allNodes) {
            if (permissionChecker.has(userId, n.getId(), Permission.READ)) {
                readSet.add(n.getId());
            }
        }
        Map<Long, List<CatalogNode>> childrenByParent = new HashMap<>();
        for (CatalogNode n : allNodes) {
            Long parent = n.getParentId();
            childrenByParent.computeIfAbsent(parent, k -> new ArrayList<>()).add(n);
        }
        List<CatalogTreeNode> tree = new ArrayList<>();
        for (CatalogNode n : allNodes) {
            if (Objects.equals(n.getParentId(), rootId)) {
                tree.add(buildSubtree(n, childrenByParent, readSet, userId));
            }
        }
        tree.removeIf(node -> !readSet.contains(node.id()));
        return new CatalogTreeView(rootId, tree);
    }

    private CatalogTreeNode buildSubtree(CatalogNode node,
                                          Map<Long, List<CatalogNode>> childrenByParent,
                                          Set<Long> readSet, long userId) {
        List<CatalogNode> children = childrenByParent.getOrDefault(node.getId(), List.of());
        List<CatalogTreeNode> visibleChildren = new ArrayList<>();
        for (CatalogNode c : children) {
            if (readSet.contains(c.getId())) {
                visibleChildren.add(buildSubtree(c, childrenByParent, readSet, userId));
            }
        }
        long knowledgeCount = knowledgeApi.countByTenantIdAndCatalogNodeId(
            node.getTenantId(), node.getId());
        return new CatalogTreeNode(node.getId(), node.getName(), node.getNodeType(),
            node.getPath(), (int) knowledgeCount, visibleChildren);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(long userId, Long nodeId, String docType, String status,
                                     String q, int page, int size) {
        if (nodeId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "nodeId 必填");
        }
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;
        permissionChecker.check(userId, nodeId, Permission.READ);

        String tenantId = TenantContext.get();
        Page<KnowledgeView> p;
        PageRequest pr = PageRequest.of(page, size);

        boolean hasDocType = docType != null && !docType.isBlank();
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasQ = q != null && !q.isBlank();

        if (hasQ) {
            String prefix = q.toLowerCase() + "%";
            p = knowledgeApi.findByTenantIdAndCatalogNodeIdAndTitlePrefix(tenantId, nodeId, prefix, pr);
        } else if (hasDocType && hasStatus) {
            p = knowledgeApi.findByTenantIdAndCatalogNodeIdAndDocTypeAndStatusOrderByCreatedAtDesc(
                tenantId, nodeId, docType, status, pr);
        } else if (hasDocType) {
            p = knowledgeApi.findByTenantIdAndCatalogNodeIdAndDocTypeOrderByCreatedAtDesc(
                tenantId, nodeId, docType, pr);
        } else if (hasStatus) {
            p = knowledgeApi.findByTenantIdAndCatalogNodeIdAndStatusOrderByCreatedAtDesc(
                tenantId, nodeId, status, pr);
        } else {
            p = knowledgeApi.findByTenantIdAndCatalogNodeIdOrderByCreatedAtDesc(
                tenantId, nodeId, pr);
        }

        List<KnowledgeListItem> items = p.getContent().stream()
            .map(KnowledgeListItem::from)
            .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("page", p.getNumber());
        result.put("size", p.getSize());
        result.put("total", p.getTotalElements());
        result.put("items", items);
        return result;
    }

    @Transactional(readOnly = true)
    public BreadcrumbView breadcrumb(long nodeId) {
        CatalogNode n = nodeStore.findById(nodeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "节点不存在"));
        String[] parts = n.getPath().replaceAll("^/|/$", "").split("/");
        List<Long> ids = new ArrayList<>();
        for (String s : parts) {
            try { ids.add(Long.parseLong(s)); } catch (NumberFormatException ignore) {}
        }
        List<CatalogNode> nodes = nodeStore.findAllById(ids);
        Map<Long, String> nameById = new HashMap<>();
        for (CatalogNode c : nodes) nameById.put(c.getId(), c.getName());
        List<BreadcrumbView.Crumb> crumbs = new ArrayList<>();
        for (Long id : ids) {
            String name = nameById.get(id);
            if (name != null) crumbs.add(new BreadcrumbView.Crumb(id, name));
        }
        return new BreadcrumbView(nodeId, crumbs);
    }
}
