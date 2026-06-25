package com.kasibridge.fraud.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI fraudServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Kasibridge Fraud Detection API")
                        .description("Fraud detection and alert management service")
                        .version("v1.0"));
    }
}
