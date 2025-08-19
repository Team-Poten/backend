package org.poten.backend.clova.dto.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClovaMediumRequest {
  private List<Message> messages;
  private double topP = 0.8;
  private Thinking thinking = Thinking.builder().effort("medium").build();
  private int topK = 60;
  private int maxCompletionTokens = 8000;
  private double temperature = 0.7;
  private double repetitionPenalty = 1.0;
  private int seed = (int) (Math.random() * 2000000000) + 1;
  private boolean includeAiFilters = true;

  public ClovaMediumRequest(List<Message> messages) {
    this.messages = messages;
  }
}
