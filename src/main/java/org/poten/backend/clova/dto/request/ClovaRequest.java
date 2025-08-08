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
    private int maxTokens = 2000;
    private double temperature = 0.5;
    private double repetitionPenalty = 0.5;
    private List<String> stop = List.of();
    private boolean includeAiFilters = true;

    public ClovaRequest(List<Message> messages) {
        this.messages = messages;
    }
}
