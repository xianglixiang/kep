package com.kep.catalog;

import java.util.List;
import java.util.Optional;

/** 目录存取端口。生产用 JPA 适配器，本地/单测用内存适配器。 */
public interface CatalogNodeStore {

    CatalogNode save(CatalogNode node);

    /** 返回当前租户下指定父节点的子节点，按 sort 升序。 */
    List<CatalogNode> findByParent(Long parentId);

    /** 当前租户下的全部节点。catalog 树接口使用。 */
    List<CatalogNode> findAll();

    /** 当前租户下按 id 查单个节点。 */
    Optional<CatalogNode> findById(Long id);

    /** 当前租户下按 id 集合查多个节点。 */
    List<CatalogNode> findAllById(List<Long> ids);
}
