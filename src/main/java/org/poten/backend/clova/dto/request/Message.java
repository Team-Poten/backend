package org.poten.backend.clova.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class Message {
    private String role;
    private List<Content> content;
}
