package org.poten.backend.question.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import org.poten.backend.clova.dto.response.QuestionDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionListResponse {
    private LocalDate date;
    private List<QuestionDto> questions;
}
