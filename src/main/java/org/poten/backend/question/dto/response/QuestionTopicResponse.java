package org.poten.backend.question.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.poten.backend.clova.dto.response.QuestionDto;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionTopicResponse {
    private String topic;
    private List<QuestionDto> questions;
}
