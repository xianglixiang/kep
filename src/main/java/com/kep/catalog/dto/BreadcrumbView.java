package com.kep.catalog.dto;

import java.util.List;

public record BreadcrumbView(Long nodeId, List<Crumb> path) {
    public record Crumb(Long id, String name) {}
}
