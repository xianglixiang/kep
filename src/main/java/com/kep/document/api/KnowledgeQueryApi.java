package com.kep.document.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * catalog 模块对 knowledge 数据的查询入口。
 * 边界: catalog 不可直接引用 {@code com.kep.document.KnowledgeRepository},
 * 只能通过本接口 (位于 {@code com.kep.document.api}) 调用,
 * Spring Modulith 的 named interface "document :: api" 允许此访问。
 */
public interface KnowledgeQueryApi {

    long countByTenantIdAndCatalogNodeId(String tenantId, Long catalogNodeId);

    Page<KnowledgeView> findByTenantIdAndCatalogNodeIdOrderByCreatedAtDesc(
        String tenantId, Long catalogNodeId, Pageable pageable);

    Page<KnowledgeView> findByTenantIdAndCatalogNodeIdAndDocTypeOrderByCreatedAtDesc(
        String tenantId, Long catalogNodeId, String docType, Pageable pageable);

    Page<KnowledgeView> findByTenantIdAndCatalogNodeIdAndStatusOrderByCreatedAtDesc(
        String tenantId, Long catalogNodeId, String status, Pageable pageable);

    Page<KnowledgeView> findByTenantIdAndCatalogNodeIdAndDocTypeAndStatusOrderByCreatedAtDesc(
        String tenantId, Long catalogNodeId, String docType, String status, Pageable pageable);

    Page<KnowledgeView> findByTenantIdAndCatalogNodeIdAndTitlePrefix(
        String tenantId, Long catalogNodeId, String titlePrefixLower, Pageable pageable);
}
