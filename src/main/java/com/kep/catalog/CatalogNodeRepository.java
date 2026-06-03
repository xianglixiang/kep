package com.kep.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface CatalogNodeRepository extends JpaRepository<CatalogNode, Long> {

    // 无需写 tenant 条件：@TenantId 由 Hibernate 自动追加
    List<CatalogNode> findByParentIdOrderBySort(Long parentId);
}
