package org.poten.backend.question.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.poten.backend.question.dto.AnswerRequest;
import org.poten.backend.question.dto.AnswerResponse;
import org.poten.backend.question.entity.Question;
import org.poten.backend.question.entity.SolveHistory;
import org.poten.backend.question.repository.QuestionRepository;
import org.poten.backend.question.repository.SolveHistoryRepository;
import org.poten.backend.user.repository.UserRepository;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizService {

    private final QuestionRepository questionRepo;
    private final SolveHistoryRepository historyRepo;
    private final UserRepository userRepo;

    public AnswerResponse submitAnswer(Long userId, Long questionId, AnswerRequest req) {
        Question q = questionRepo.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("question not found: " + questionId));

        String normalizedUser = normalize(req.getUserAnswer());
        String normalizedAnswer = normalize(q.getAnswer());

        boolean correct = normalizedUser.equalsIgnoreCase(normalizedAnswer);

        SolveHistory h = new SolveHistory();
        h.setUser(userRepo.getReferenceById(userId));
        h.setQuestion(q);
        h.setIsTrue(correct);
        h.setUser_answer(normalizedUser);

        historyRepo.save(h);

        AnswerResponse res = new AnswerResponse();
        res.setCorrect(correct);
        res.setCorrectAnswer(normalizedAnswer);
        res.setExplanation(q.getExplanation());
        res.setQuestionId(q.getId());
        return res;
    }

    private String normalize(String a) {
        if (a == null) return "";
        String s = a.trim().toUpperCase();
        if ("O".equals(s) || "TRUE".equals(s))  return "TRUE";
        if ("X".equals(s) || "FALSE".equals(s)) return "FALSE";
        throw new IllegalArgumentException("answer must be O/X/TRUE/FALSE");
    }
}
