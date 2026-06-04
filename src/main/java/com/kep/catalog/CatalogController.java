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

    public CatalogController(CatalogService service) {
        this.service = service;
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

    private Long requireUserId() {
        Long userId = SecurityContext.getCurrentUserId();
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        return userId;
    }
}
