package com.kep.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface KnowledgeVersionRepository extends JpaRepository<KnowledgeVersion, Long> {

    List<KnowledgeVersion> findByKnowledgeIdOrderByVersionNoDesc(Long knowledgeId);

    Optional<KnowledgeVersion> findByKnowledgeIdAndVersionNo(Long knowledgeId, int versionNo);
}
