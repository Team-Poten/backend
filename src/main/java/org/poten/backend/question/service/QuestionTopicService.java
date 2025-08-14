package org.poten.backend.question.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.poten.backend.global.error.ErrorCode;
import org.poten.backend.global.exception.CustomException;
import org.poten.backend.question.dto.request.TopicUpdateRequestDto;
import org.poten.backend.question.entity.Question;
import org.poten.backend.question.repository.QuestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionTopicService {

    private final QuestionRepository questionRepository;

    @Transactional
    public void updateQuestionTopic(TopicUpdateRequestDto requestDto) {
        List<Long> requestedIds = requestDto.getQuestionIds();
        List<Question> foundQuestions = questionRepository.findAllById(requestedIds);
        if (foundQuestions.size() != requestedIds.size()) {
            throw new QuestionTopicServiceException(QuestionTopicErrorCode.QUESTION_NOT_FOUND);
        }
        for (Question question : foundQuestions) {
            question.setTopic(requestDto.getTopic());
        }
        questionRepository.saveAll(foundQuestions);
    }

    @Getter
    @RequiredArgsConstructor
    public enum QuestionTopicErrorCode implements ErrorCode {
        QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Q001", "존재하지 않는 문제입니다.");

        private final HttpStatus httpStatus;
        private final String code;
        private final String message;
    }

    public static class QuestionTopicServiceException extends CustomException {
        public QuestionTopicServiceException(ErrorCode errorCode) {
            super(errorCode);
        }
    }
}
