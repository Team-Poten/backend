package org.poten.backend.clova.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ClovaRequest {
    private List<Message> messages;
    private double topP = 0.8;
    private int topK = 0;
    private int maxTokens = 3000;
    private double temperature = 0.3;
    private double repetitionPenalty = 1.0;
    private List<String> stop = List.of();
    private int seed = (int) (Math.random() * 2000000000) + 1;
    private boolean includeAiFilters = true;

    public ClovaRequest(List<Message> messages) {
        this.messages = messages;
    }
}
