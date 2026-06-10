package com.kep.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface KnowledgeRepository extends JpaRepository<Knowledge, Long> {

    List<Knowledge> findByCatalogNodeIdOrderByCreatedAtDesc(Long catalogNodeId);
}
