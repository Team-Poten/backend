package org.poten.backend.global.infra.ocr;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ocr.api")
public class OcrProperty {
    private String url;
    private String secret;
}
