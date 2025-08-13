package org.poten.backend.clova.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrRequestDto {
    private List<Image> images;
    private String requestId;
    private String version;
    private long timestamp;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Image {
        private String format;
        private String name;
    }
}
