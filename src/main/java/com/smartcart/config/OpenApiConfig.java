package com.smartcart.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartCartOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SmartCart API")
                        .description("Grocery receipt scanner and expense analytics API")
                        .version("1.0.0"));
    }
}
