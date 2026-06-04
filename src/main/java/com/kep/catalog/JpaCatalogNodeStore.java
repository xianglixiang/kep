package com.kep.catalog;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("!local-mock")
@ConditionalOnBean(CatalogNodeRepository.class)
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
