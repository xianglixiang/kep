package com.kep.catalog;

import com.kep.catalog.dto.CreateNodeRequest;
import com.kep.catalog.dto.NodeView;
import com.kep.shared.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/nodes")
public class CatalogController {

    private final CatalogService service;

    public CatalogController(CatalogService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<NodeView> create(@Valid @RequestBody CreateNodeRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    @GetMapping
    public ApiResponse<List<NodeView>> list(@RequestParam(required = false) Long parentId) {
        return ApiResponse.ok(service.listChildren(parentId));
    }
}
