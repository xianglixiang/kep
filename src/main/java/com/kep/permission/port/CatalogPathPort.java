package com.kep.permission.port;

import java.util.List;

public interface CatalogPathPort {

    /**
     * 返回 catalog_node 自身的 id 与其所有祖先 id，按"从远到近"排序（含自身），
     * 例如 path='/1/4/9/' → [1, 4, 9]。
     */
    List<Long> ancestorIds(long catalogNodeId);
}
