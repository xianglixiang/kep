package com.kep.catalog;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

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

    @Override
    public List<CatalogNode> findAll() {
        return repo.findAll();
    }

    @Override
    public Optional<CatalogNode> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public List<CatalogNode> findAllById(List<Long> ids) {
        return repo.findAllById(ids);
    }
}
