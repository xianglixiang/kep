package com.kep.document;

import com.kep.document.api.KnowledgeQueryApi;
import com.kep.document.api.KnowledgeView;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * 仅生产剖面启用——与 {@link DocumentService} 同模式，
 * local-mock 排除 JpaRepositoriesAutoConfiguration，无 {@code KnowledgeRepository} 可注入。
 */
@Service
@Profile("!local-mock")
public class KnowledgeQueryApiImpl implements KnowledgeQueryApi {

    private final KnowledgeRepository knowledgeRepo;

    public KnowledgeQueryApiImpl(KnowledgeRepository knowledgeRepo) {
        this.knowledgeRepo = knowledgeRepo;
    }

    @Override
    public long countByTenantIdAndCatalogNodeId(String tenantId, Long catalogNodeId) {
        return knowledgeRepo.countByTenantIdAndCatalogNodeId(tenantId, catalogNodeId);
    }

    @Override
    public Page<KnowledgeView> findByTenantIdAndCatalogNodeIdOrderByCreatedAtDesc(
        String tenantId, Long catalogNodeId, Pageable pageable) {
        return knowledgeRepo
            .findByTenantIdAndCatalogNodeIdOrderByCreatedAtDesc(tenantId, catalogNodeId, pageable)
            .map(KnowledgeView::from);
    }

    @Override
    public Page<KnowledgeView> findByTenantIdAndCatalogNodeIdAndDocTypeOrderByCreatedAtDesc(
        String tenantId, Long catalogNodeId, String docType, Pageable pageable) {
        return knowledgeRepo
            .findByTenantIdAndCatalogNodeIdAndDocTypeOrderByCreatedAtDesc(tenantId, catalogNodeId, docType, pageable)
            .map(KnowledgeView::from);
    }

    @Override
    public Page<KnowledgeView> findByTenantIdAndCatalogNodeIdAndStatusOrderByCreatedAtDesc(
        String tenantId, Long catalogNodeId, String status, Pageable pageable) {
        return knowledgeRepo
            .findByTenantIdAndCatalogNodeIdAndStatusOrderByCreatedAtDesc(tenantId, catalogNodeId, status, pageable)
            .map(KnowledgeView::from);
    }

    @Override
    public Page<KnowledgeView> findByTenantIdAndCatalogNodeIdAndDocTypeAndStatusOrderByCreatedAtDesc(
        String tenantId, Long catalogNodeId, String docType, String status, Pageable pageable) {
        return knowledgeRepo
            .findByTenantIdAndCatalogNodeIdAndDocTypeAndStatusOrderByCreatedAtDesc(tenantId, catalogNodeId, docType, status, pageable)
            .map(KnowledgeView::from);
    }

    @Override
    public Page<KnowledgeView> findByTenantIdAndCatalogNodeIdAndTitlePrefix(
        String tenantId, Long catalogNodeId, String titlePrefixLower, Pageable pageable) {
        return knowledgeRepo
            .findByTenantIdAndCatalogNodeIdAndTitlePrefix(tenantId, catalogNodeId, titlePrefixLower, pageable)
            .map(KnowledgeView::from);
    }
}
