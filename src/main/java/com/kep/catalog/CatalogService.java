package com.kep.catalog;

import com.kep.catalog.dto.CreateNodeRequest;
import com.kep.catalog.dto.NodeView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogService {

    private final CatalogNodeStore store;

    public CatalogService(CatalogNodeStore store) {
        this.store = store;
    }

    @Transactional
    public NodeView create(CreateNodeRequest req) {
        CatalogNode saved = store.save(CatalogNode.folder(req.parentId(), req.name(), "/"));
        return NodeView.from(saved);
    }

    @Transactional(readOnly = true)
    public List<NodeView> listChildren(Long parentId) {
        return store.findByParent(parentId).stream().map(NodeView::from).toList();
    }
}
