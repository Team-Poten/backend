package org.poten.backend.question.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.poten.backend.question.entity.Question;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    private String questionText;
    private String answer;
    private String questionType;
    private String explanation;

    public static QuestionResponse from(Question question) {
        return QuestionResponse.builder()
                .questionText(question.getQuestionText())
                .answer(question.getAnswer())
                .questionType(question.getQuestionType())
                .explanation(question.getExplanation())
                .build();
    }
}
