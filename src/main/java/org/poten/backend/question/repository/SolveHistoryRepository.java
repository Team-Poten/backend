package org.poten.backend.question.repository;

import org.poten.backend.question.entity.SolveHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolveHistoryRepository extends JpaRepository<SolveHistory, Long> {
}
