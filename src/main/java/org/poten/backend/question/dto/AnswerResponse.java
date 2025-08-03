package org.poten.backend.question.dto;

import lombok.Getter;
import lombok.Setter;

/* 채점 응답 dto */
@Getter
@Setter
public class AnswerResponse {
    private boolean correct;
    private String correctAnswer;     // "TRUE" | "FALSE"
    private String explanation;
    private Long questionId;
}