package com.kep.review;

import com.kep.review.dto.DecideReviewRequest;
import com.kep.review.dto.SubmitReviewRequest;
import com.kep.permission.api.Permission;
import com.kep.permission.api.PermissionChecker;
import com.kep.shared.error.BusinessException;
import com.kep.shared.error.ErrorCode;
import com.kep.shared.tenant.TenantContext;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * M1 骨架：落 submit / decide 调用与鉴权，不实现状态机（status 始终 PENDING，留 M4 补）。
 * M1 阶段 knowledgeId 视作 catalog_node_id；M2 引入 knowledge 表后该列改为外键。
 * 仅生产剖面启用——local-mock 排除 DataSource，无 JdbcTemplate 可注入。
 */
@Service
@Profile("!local-mock")
public class ReviewService {

    private final JdbcTemplate jdbc;
    private final PermissionChecker permissionChecker;

    public ReviewService(JdbcTemplate jdbc, PermissionChecker permissionChecker) {
        this.jdbc = jdbc;
        this.permissionChecker = permissionChecker;
    }

    @Transactional
    public long submit(long userId, SubmitReviewRequest req) {
        permissionChecker.check(userId, req.knowledgeId(), Permission.WRITE);

        String tenant = TenantContext.get();
        if (tenant == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少租户上下文");
        }
        jdbc.update(
            "INSERT INTO review_request (tenant_id, knowledge_id, version_id, submitter_id, " +
            "reviewer_subject_type, reviewer_subject_id) VALUES (?, ?, ?, ?, ?, ?)",
            tenant, req.knowledgeId(), req.versionId(), userId,
            req.reviewerSubjectType(), req.reviewerSubjectId());
        return jdbc.queryForObject("SELECT lastval()", Long.class);
    }

    @Transactional
    public void decide(long userId, DecideReviewRequest req) {
        var rs = jdbc.queryForList(
            "SELECT tenant_id, knowledge_id, reviewer_subject_type, reviewer_subject_id " +
            "FROM review_request WHERE id = ?", req.reviewId());
        if (rs.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "审核请求不存在");
        }
        var row = rs.get(0);
        String subjectType = (String) row.get("reviewer_subject_type");
        Long subjectId = ((Number) row.get("reviewer_subject_id")).longValue();
        long knowledgeId = ((Number) row.get("knowledge_id")).longValue();

        // M1 简化：仅 subject_type=USER 可决定；ORG_UNIT 留 M4 工作流补
        if (!"USER".equals(subjectType) || subjectId != userId) {
            throw new AccessDeniedException("仅指定审核者可决定");
        }
        permissionChecker.check(userId, knowledgeId, Permission.WRITE);

        // M1 不切 status，仅记录决定
        jdbc.update(
            "UPDATE review_request SET decided_at = now(), opinion = ? WHERE id = ?",
            req.opinion(), req.reviewId());
    }
}
