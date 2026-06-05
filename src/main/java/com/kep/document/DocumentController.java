package com.kep.document;

import com.kep.document.dto.KnowledgeVersionView;
import com.kep.document.dto.KnowledgeView;
import com.kep.shared.error.BusinessException;
import com.kep.shared.error.ErrorCode;
import com.kep.shared.security.SecurityContext;
import com.kep.shared.web.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 仅生产剖面启用——与 {@link DocumentService} 对齐，local-mock 下不创建（无 KnowledgeRepository）。
 */
@RestController
@RequestMapping("/api/knowledge")
@Profile("!local-mock")
public class DocumentController {

    private static final MediaType DOCX_MEDIA_TYPE = MediaType.parseMediaType(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<KnowledgeView> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("catalogNodeId") Long catalogNodeId,
            @RequestParam(value = "title", required = false) String title) throws Exception {
        Long userId = requireUserId();
        return ApiResponse.ok(service.upload(userId, catalogNodeId, file, title));
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeView> getCurrent(@PathVariable Long id) {
        Long userId = requireUserId();
        return ApiResponse.ok(service.getCurrent(userId, id));
    }

    @GetMapping("/{id}/versions/{n}")
    public ApiResponse<KnowledgeVersionView> getVersion(
            @PathVariable Long id, @PathVariable int n) {
        Long userId = requireUserId();
        return ApiResponse.ok(service.getVersion(userId, id, n));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> getOriginalFile(@PathVariable Long id) {
        Long userId = requireUserId();
        byte[] bytes = service.getOriginalFile(userId, id);
        return ResponseEntity.ok()
            .contentType(DOCX_MEDIA_TYPE)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"knowledge-" + id + ".docx\"")
            .body(bytes);
    }

    private Long requireUserId() {
        Long userId = SecurityContext.getCurrentUserId();
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        return userId;
    }
}
