package com.kep.catalog;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 生产 catalog 存储：默认剖面（启用 JPA/Flyway）。在 local-mock 剖面下不激活，
 * 由 InMemoryCatalogNodeStore 替代。
 */
@Component
@Profile("!local-mock")
class JpaCatalogNodeStore implements CatalogNodeStore {

    private final CatalogNodeRepository repo;

    JpaCatalogNodeStore(CatalogNodeRepository repo) {
        this.repo = repo;
    }

    @Override
    public CatalogNode save(CatalogNode node) {
        return repo.save(node);
    }

    @Override
    public List<CatalogNode> findByParent(Long parentId) {
        return repo.findByParentIdOrderBySort(parentId);
    }
}
