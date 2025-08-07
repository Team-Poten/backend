package org.poten.backend.question.service;

import org.poten.backend.clova.dto.response.QuestionDto;
import lombok.RequiredArgsConstructor;
import org.poten.backend.global.error.GlobalErrorCode;
import org.poten.backend.global.exception.CustomException;
import org.poten.backend.question.dto.response.QuestionListResponse;
import org.poten.backend.question.entity.Question;
import org.poten.backend.question.repository.QuestionRepository;
import org.poten.backend.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionService {

    private final QuestionRepository questionRepository;

    public List<QuestionListResponse> findAllQuestion(User user) {
        if (user == null) {
            throw new CustomException(GlobalErrorCode.USER_NOT_FOUND);
        }

        List<Question> questions = questionRepository.findByUser(user);

        Map<LocalDate, List<QuestionDto>> groupedByDate = questions.stream()
                .collect(Collectors.groupingBy(
                        question -> question.getCreatedAt().toLocalDate(),
                        Collectors.mapping(QuestionDto::from, Collectors.toList())
                ));

        return groupedByDate.entrySet().stream()
                .map(entry -> QuestionListResponse.builder()
                        .date(entry.getKey())
                        .questions(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }
}
