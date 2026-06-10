package com.kep.catalog;

import com.kep.catalog.dto.CreateNodeRequest;
import com.kep.catalog.dto.NodeView;
import com.kep.shared.error.BusinessException;
import com.kep.shared.error.ErrorCode;
import com.kep.shared.security.SecurityContext;
import com.kep.shared.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/nodes")
public class CatalogController {

    private final CatalogService service;
    private final KnowledgeQueryService knowledgeQueryService;

    public CatalogController(CatalogService service,
                             KnowledgeQueryService knowledgeQueryService) {
        this.service = service;
        this.knowledgeQueryService = knowledgeQueryService;
    }

    @PostMapping
    public ApiResponse<NodeView> create(@Valid @RequestBody CreateNodeRequest req) {
        Long userId = requireUserId();
        return ApiResponse.ok(service.create(userId, req));
    }

    @GetMapping
    public ApiResponse<List<NodeView>> list(@RequestParam(required = false) Long parentId) {
        Long userId = requireUserId();
        return ApiResponse.ok(service.listChildren(userId, parentId));
    }

    @GetMapping("/tree")
    public ApiResponse<com.kep.catalog.dto.CatalogTreeView> tree(
            @RequestParam(required = false) Long rootId) {
        Long userId = requireUserId();
        return ApiResponse.ok(knowledgeQueryService.tree(userId, rootId));
    }

    @GetMapping("/knowledge")
    public ApiResponse<java.util.Map<String, Object>> listKnowledge(
            @RequestParam Long nodeId,
            @RequestParam(required = false) String docType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = requireUserId();
        return ApiResponse.ok(knowledgeQueryService.list(userId, nodeId, docType, status, q, page, size));
    }

    @GetMapping("/breadcrumb")
    public ApiResponse<com.kep.catalog.dto.BreadcrumbView> breadcrumb(
            @RequestParam Long nodeId) {
        Long userId = requireUserId();
        return ApiResponse.ok(knowledgeQueryService.breadcrumb(nodeId));
    }

    private Long requireUserId() {
        Long userId = SecurityContext.getCurrentUserId();
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        return userId;
    }
}
