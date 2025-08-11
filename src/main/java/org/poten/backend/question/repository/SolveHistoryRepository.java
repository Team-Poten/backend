package org.poten.backend.question.repository;

import org.poten.backend.question.entity.Question;
import org.poten.backend.question.entity.SolveHistory;
import org.poten.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SolveHistoryRepository extends JpaRepository<SolveHistory, Long> {
    Page<SolveHistory> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    boolean existsByUser_IdAndQuestion_Id(Long userId, Long questionId);

    @Query("SELECT sh FROM SolveHistory sh WHERE sh.user = :user AND sh.question IN :questions AND sh.createdAt = (SELECT MAX(sh2.createdAt) FROM SolveHistory sh2 WHERE sh2.user = sh.user AND sh2.question = sh.question)")
    List<SolveHistory> findLatestSolveHistories(@Param("user") User user, @Param("questions") List<Question> questions);
}

