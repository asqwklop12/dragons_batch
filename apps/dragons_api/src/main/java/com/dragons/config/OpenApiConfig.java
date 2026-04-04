package com.dragons.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI dragonsOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Dragons Batch API")
            .description("KAMIS 가격 조회 및 배치 실행 API 문서")
            .version("v0.0.1")
            .license(new License().name("Internal").url("https://example.local")));
  }
}
