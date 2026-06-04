package com.kep.review;

import com.kep.review.dto.DecideReviewRequest;
import com.kep.review.dto.SubmitReviewRequest;
import com.kep.shared.error.BusinessException;
import com.kep.shared.error.ErrorCode;
import com.kep.shared.security.SecurityContext;
import com.kep.shared.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/**
 * 仅生产剖面启用——与 {@link ReviewService} 对齐，local-mock 下不创建（无 JdbcTemplate）。
 */
@RestController
@RequestMapping("/api/reviews")
@Profile("!local-mock")
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<Long> submit(@Valid @RequestBody SubmitReviewRequest req) {
        Long userId = requireUserId();
        return ApiResponse.ok(service.submit(userId, req));
    }

    @PostMapping("/decide")
    public ApiResponse<Void> decide(@Valid @RequestBody DecideReviewRequest req) {
        Long userId = requireUserId();
        service.decide(userId, req);
        return ApiResponse.ok(null);
    }

    private Long requireUserId() {
        Long userId = SecurityContext.getCurrentUserId();
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        return userId;
    }
}
