package com.kep.catalog.dto;

import com.kep.catalog.CatalogNode;

public record NodeView(Long id, Long parentId, String name, String nodeType) {

    public static NodeView from(CatalogNode node) {
        return new NodeView(node.getId(), node.getParentId(), node.getName(), node.getNodeType());
    }
}
