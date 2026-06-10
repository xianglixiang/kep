package com.kep.catalog.dto;

import java.util.List;

public record CatalogTreeNode(
    Long id, String name, String nodeType, String path, int knowledgeCount,
    List<CatalogTreeNode> children
) {}
