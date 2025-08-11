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



        boolean correct = isCorrect(q,req.getUserAnswer());

        SolveHistory h = new SolveHistory();
        h.setUser(userRepo.getReferenceById(userId));
        h.setQuestion(q);
        h.setIsTrue(correct);
        h.setUser_answer(req.getUserAnswer());

        historyRepo.save(h);

        AnswerResponse res = new AnswerResponse();
        res.setCorrect(correct);
        res.setCorrectAnswer(q.getAnswer());
        res.setExplanation(q.getExplanation());
        res.setQuestionId(q.getId());
        return res;
    }

    /* 비회원 전용 로직 */
    public AnswerResponse submitAnswerWithoutSave(Long questionId, AnswerRequest req) {
        Question q = questionRepo.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("question not found: " + questionId));


        boolean correct = isCorrect(q,req.getUserAnswer());

        AnswerResponse res = new AnswerResponse();
        res.setCorrect(correct);
        res.setCorrectAnswer(q.getAnswer());
        res.setExplanation(q.getExplanation());
        res.setQuestionId(q.getId());
        return res;
    }

    private boolean isCorrect(Question q, String userInputRaw){
        String userAN = normalizeText(userInputRaw);
        return userAN.equals(q.getAnswer());

    }



    private String normalizeText(String a){
        if(a== null) return "";
        return a.trim().toUpperCase();
    }
}
