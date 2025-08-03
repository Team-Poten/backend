package org.poten.backend.global.infra.clova;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("clova.api")
public class ClovaProperty {
    private String url;
    private String key;
    private String id;
}
