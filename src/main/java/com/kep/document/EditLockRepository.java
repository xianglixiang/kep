package com.kep.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface EditLockRepository extends JpaRepository<EditLock, Long> {

    Optional<EditLock> findByKnowledgeId(Long knowledgeId);
}
