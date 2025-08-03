package org.poten.backend.question.repository;

import org.poten.backend.question.entity.SolveHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolveHistoryRepository extends JpaRepository<SolveHistory, Long> {
    Page<SolveHistory> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    boolean existsByUser_IdAndQuestion_Id(Long userId, Long questionId);
}

