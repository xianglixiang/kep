package com.kep.catalog.dto;

import java.util.List;

public record CatalogTreeView(Long rootId, List<CatalogTreeNode> tree) {}
