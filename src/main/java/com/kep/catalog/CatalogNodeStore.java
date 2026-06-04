package com.kep.catalog;

import java.util.List;

/** 目录存取端口。生产用 JPA 适配器，本地/单测用内存适配器。 */
public interface CatalogNodeStore {

    CatalogNode save(CatalogNode node);

    /** 返回当前租户下指定父节点的子节点，按 sort 升序。 */
    List<CatalogNode> findByParent(Long parentId);
}
