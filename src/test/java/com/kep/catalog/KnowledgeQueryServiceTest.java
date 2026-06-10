package com.kep.catalog;

import com.kep.catalog.dto.BreadcrumbView;
import com.kep.catalog.dto.CatalogTreeView;
import com.kep.document.api.KnowledgeQueryApi;
import com.kep.permission.api.Permission;
import com.kep.permission.api.PermissionChecker;
import com.kep.shared.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeQueryServiceTest {

    private KnowledgeQueryApi knowledgeApi;
    private TestCatalogNodeStore nodeStore;
    private PermissionChecker permissionChecker;
    private KnowledgeQueryService service;

    @BeforeEach
    void setup() {
        knowledgeApi = mock(KnowledgeQueryApi.class);
        nodeStore = new TestCatalogNodeStore();
        permissionChecker = mock(PermissionChecker.class);
        service = new KnowledgeQueryService(knowledgeApi, nodeStore, permissionChecker);
        TenantContext.set("tenant-a");
        when(permissionChecker.has(anyLong(), anyLong(), any())).thenReturn(true);
        when(knowledgeApi.countByTenantIdAndCatalogNodeId(any(), anyLong())).thenReturn(0L);
    }

    @AfterEach
    void clear() { TenantContext.clear(); }

    @Test
    void tree_returns_root_with_its_visible_children() {
        CatalogNode root = makeNode(1L, null, "产品部", "/1/");
        CatalogNode c1 = makeNode(2L, 1L, "子1", "/1/2/");
        CatalogNode c2 = makeNode(3L, 1L, "子2", "/1/3/");
        CatalogNode gc = makeNode(4L, 2L, "孙", "/1/2/4/");
        nodeStore.add(root, c1, c2, gc);

        CatalogTreeView view = service.tree(100L, 1L);

        assertThat(view.rootId()).isEqualTo(1L);
        assertThat(view.tree()).hasSize(2);
        assertThat(view.tree().get(0).name()).isEqualTo("子1");
        assertThat(view.tree().get(0).children()).hasSize(1);
    }

    @Test
    void tree_filters_out_nodes_without_read_permission() {
        CatalogNode root = makeNode(1L, null, "root", "/1/");
        CatalogNode hidden = makeNode(2L, 1L, "secret", "/1/2/");
        CatalogNode visible = makeNode(3L, 1L, "open", "/1/3/");
        nodeStore.add(root, hidden, visible);
        when(permissionChecker.has(eq(100L), eq(2L), eq(Permission.READ))).thenReturn(false);
        when(permissionChecker.has(eq(100L), eq(3L), eq(Permission.READ))).thenReturn(true);
        when(permissionChecker.has(eq(100L), eq(1L), eq(Permission.READ))).thenReturn(true);

        CatalogTreeView view = service.tree(100L, 1L);

        assertThat(view.tree()).hasSize(1);
        assertThat(view.tree().get(0).name()).isEqualTo("open");
    }

    @Test
    void tree_root_without_read_returns_empty() {
        CatalogNode root = makeNode(1L, null, "root", "/1/");
        nodeStore.add(root);
        when(permissionChecker.has(anyLong(), eq(1L), eq(Permission.READ))).thenReturn(false);

        CatalogTreeView view = service.tree(100L, 1L);

        assertThat(view.tree()).isEmpty();
    }

    @Test
    void breadcrumb_parses_path() {
        CatalogNode node = makeNode(4L, 2L, "孙", "/1/2/4/");
        nodeStore.add(
            makeNode(1L, null, "root", "/1/"),
            makeNode(2L, 1L, "子", "/1/2/"),
            node
        );

        BreadcrumbView view = service.breadcrumb(4L);

        assertThat(view.nodeId()).isEqualTo(4L);
        assertThat(view.path()).hasSize(3);
        assertThat(view.path().get(0).name()).isEqualTo("root");
        assertThat(view.path().get(1).name()).isEqualTo("子");
        assertThat(view.path().get(2).name()).isEqualTo("孙");
    }

    private CatalogNode makeNode(long id, Long parentId, String name, String path) {
        CatalogNode n = new CatalogNode();
        try {
            var idF = CatalogNode.class.getDeclaredField("id");
            idF.setAccessible(true);
            idF.set(n, id);
        } catch (Exception e) { throw new RuntimeException(e); }
        n.assignId(id);
        try {
            var pF = CatalogNode.class.getDeclaredField("parentId");
            pF.setAccessible(true);
            pF.set(n, parentId);
        } catch (Exception e) { throw new RuntimeException(e); }
        try {
            var nmF = CatalogNode.class.getDeclaredField("name");
            nmF.setAccessible(true);
            nmF.set(n, name);
        } catch (Exception e) { throw new RuntimeException(e); }
        n.assignPath(path);
        try {
            Class<?> c = CatalogNode.class;
            java.lang.reflect.Field tF = null;
            while (c != null) {
                try { tF = c.getDeclaredField("tenantId"); break; }
                catch (NoSuchFieldException ignore) { c = c.getSuperclass(); }
            }
            if (tF == null) throw new NoSuchFieldException("tenantId");
            tF.setAccessible(true);
            tF.set(n, "tenant-a");
        } catch (Exception e) { throw new RuntimeException(e); }
        return n;
    }

    /**
     * 内存版 CatalogNodeStore，单测用：保存预置节点并按需响应 find* 调用。
     * 替代了原先用 Mockito mock CatalogNodeRepository 的方案。
     */
    private static class TestCatalogNodeStore implements CatalogNodeStore {
        private final Map<Long, CatalogNode> nodes = new HashMap<>();

        void add(CatalogNode... ns) {
            for (CatalogNode n : ns) nodes.put(n.getId(), n);
        }

        @Override
        public CatalogNode save(CatalogNode node) {
            nodes.put(node.getId(), node);
            return node;
        }

        @Override
        public List<CatalogNode> findByParent(Long parentId) {
            List<CatalogNode> out = new ArrayList<>();
            for (CatalogNode n : nodes.values()) {
                if (Objects.equals(n.getParentId(), parentId)) out.add(n);
            }
            return out;
        }

        @Override
        public List<CatalogNode> findAll() {
            return new ArrayList<>(nodes.values());
        }

        @Override
        public Optional<CatalogNode> findById(Long id) {
            return Optional.ofNullable(nodes.get(id));
        }

        @Override
        public List<CatalogNode> findAllById(List<Long> ids) {
            List<CatalogNode> out = new ArrayList<>();
            for (Long id : ids) {
                CatalogNode n = nodes.get(id);
                if (n != null) out.add(n);
            }
            return out;
        }
    }
}
