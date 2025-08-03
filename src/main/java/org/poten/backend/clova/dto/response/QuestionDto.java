package org.poten.backend.clova.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class QuestionDto {
    private String question;
    private String type;
    private List<String> options;
    private String answer;
    private String explanation;
}
