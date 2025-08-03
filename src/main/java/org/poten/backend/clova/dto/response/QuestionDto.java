package org.poten.backend.clova.dto.response;

import java.util.Collections;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.poten.backend.question.entity.Question;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDto {
    private Long questionId;
    private String question;
    private String type;
    private List<String> options;
    private String answer;
    private String explanation;

    public static QuestionDto from(Question question) {
        return new QuestionDto(
                question.getId(),
                question.getQuestionText(),
                question.getQuestionType(),
                Collections.emptyList(),
                question.getAnswer(),
                question.getExplanation()
        );
    }
}
