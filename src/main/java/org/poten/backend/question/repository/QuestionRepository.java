package org.poten.backend.question.repository;

import org.poten.backend.question.entity.Question;
import org.poten.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByUser(User user);
}
