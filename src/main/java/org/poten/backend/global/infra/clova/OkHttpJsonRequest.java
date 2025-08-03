package org.poten.backend.global.infra.clova;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OkHttpJsonRequest extends OkHttpRequest {

    private final Object request;

    public OkHttpJsonRequest(Object request) {
        this.request = request;
    }

    @Override
    public String convertRequestToString() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this.request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
