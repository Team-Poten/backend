package org.poten.backend.clova.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.poten.backend.question.entity.Question;
import org.poten.backend.question.entity.SolveHistory;

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String latestSolveStatus;
    private String topic;

    public static QuestionDto from(Question question, Optional<SolveHistory> solveHistory) {
        String status = solveHistory
                .map(history -> history.getIsTrue() ? "CORRECT" : "INCORRECT")
                .orElse("UNSOLVED");

        return new QuestionDto(
                question.getId(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getOptions(),
                question.getAnswer(),
                question.getExplanation(),
                question.getCreatedAt(),
                question.getUpdatedAt(),
                status,
                question.getTopic()
        );
    }
}
