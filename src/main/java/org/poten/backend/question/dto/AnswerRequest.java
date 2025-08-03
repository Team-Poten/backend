package org.poten.backend.question.dto;

import lombok.Getter;

/* 채점 요청 dto */
@Getter
public class AnswerRequest {
    private String userAnswer;        // "O" | "X" | "TRUE" | "FALSE"

}