package org.poten.backend.clova.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OcrResponseDto {
    private List<ImageResult> images;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageResult {
        private List<Field> fields;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Field {
        private String inferText;
    }

    public String getFullText() {
        if (images == null || images.isEmpty()) {
            return "";
        }
        return images.stream()
                .flatMap(imageResult -> imageResult.getFields().stream())
                .map(Field::getInferText)
                .collect(Collectors.joining(" "));
    }
}
