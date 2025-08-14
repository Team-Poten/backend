package org.poten.backend.question.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TopicUpdateRequestDto {
    private String topic;
    private List<Long> questionIds;
}
