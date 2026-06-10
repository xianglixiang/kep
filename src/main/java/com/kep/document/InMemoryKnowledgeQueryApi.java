package com.kep.document;

import com.kep.document.api.KnowledgeQueryApi;
import com.kep.document.api.KnowledgeView;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * local-mock 下的 {@link KnowledgeQueryApi} 桩实现：始终返回空。
 * 对应 JpaRepositoriesAutoConfiguration 被排除、无 KnowledgeRepository 可注入的场景。
 * 真实查询在生产剖面下由 {@link KnowledgeQueryApiImpl} 提供。
 */
@Service
@Profile("local-mock")
public class InMemoryKnowledgeQueryApi implements KnowledgeQueryApi {

    @Override
    public long countByTenantIdAndCatalogNodeId(String tenantId, Long catalogNodeId) {
        return 0L;
    }

    @Override
    public Page<KnowledgeView> findByTenantIdAndCatalogNodeIdOrderByCreatedAtDesc(
        String tenantId, Long catalogNodeId, Pageable pageable) {
        return empty(pageable);
    }

    @Override
    public Page<KnowledgeView> findByTenantIdAndCatalogNodeIdAndDocTypeOrderByCreatedAtDesc(
        String tenantId, Long catalogNodeId, String docType, Pageable pageable) {
        return empty(pageable);
    }

    @Override
    public Page<KnowledgeView> findByTenantIdAndCatalogNodeIdAndStatusOrderByCreatedAtDesc(
        String tenantId, Long catalogNodeId, String status, Pageable pageable) {
        return empty(pageable);
    }

    @Override
    public Page<KnowledgeView> findByTenantIdAndCatalogNodeIdAndDocTypeAndStatusOrderByCreatedAtDesc(
        String tenantId, Long catalogNodeId, String docType, String status, Pageable pageable) {
        return empty(pageable);
    }

    @Override
    public Page<KnowledgeView> findByTenantIdAndCatalogNodeIdAndTitlePrefix(
        String tenantId, Long catalogNodeId, String titlePrefixLower, Pageable pageable) {
        return empty(pageable);
    }

    private static Page<KnowledgeView> empty(Pageable pageable) {
        return new PageImpl<>(List.of(), pageable, 0);
    }
}
