package com.example.batch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI springBatchOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Batch Sample API")
                        .description("Sample Batch jobs trigger API")
                        .version("v1"));
    }
}



