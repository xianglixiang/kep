package com.kep.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface KnowledgeRepository extends JpaRepository<Knowledge, Long> {

    List<Knowledge> findByCatalogNodeIdOrderByCreatedAtDesc(Long catalogNodeId);

    // ---- M3 加: 列表过滤 ----

    Page<Knowledge> findByTenantIdAndCatalogNodeIdOrderByCreatedAtDesc(
        String tenantId, Long catalogNodeId, Pageable pageable);

    Page<Knowledge> findByTenantIdAndCatalogNodeIdAndDocTypeOrderByCreatedAtDesc(
        String tenantId, Long catalogNodeId, String docType, Pageable pageable);

    Page<Knowledge> findByTenantIdAndCatalogNodeIdAndStatusOrderByCreatedAtDesc(
        String tenantId, Long catalogNodeId, String status, Pageable pageable);

    Page<Knowledge> findByTenantIdAndCatalogNodeIdAndDocTypeAndStatusOrderByCreatedAtDesc(
        String tenantId, Long catalogNodeId, String docType, String status, Pageable pageable);

    /**
     * 标题前模糊搜索 (lowercase match). q 形如 "foo%".
     * tenant_id + nodeId + lower(title) 三者都需匹配.
     */
    @Query("SELECT k FROM Knowledge k WHERE k.tenantId = ?1 AND k.catalogNodeId = ?2 " +
           "AND LOWER(k.title) LIKE ?3 ORDER BY k.createdAt DESC")
    Page<Knowledge> findByTenantIdAndCatalogNodeIdAndTitlePrefix(
        String tenantId, Long catalogNodeId, String titlePrefixLower, Pageable pageable);

    /** 同一节点下 knowledge 数量 (树接口显示 knowledgeCount 用). */
    @Query("SELECT COUNT(k) FROM Knowledge k WHERE k.tenantId = ?1 AND k.catalogNodeId = ?2")
    long countByTenantIdAndCatalogNodeId(String tenantId, Long catalogNodeId);
}
