package com.krish.issuetracker.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI issueTrackerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Issue Tracker API")
                        .version("v1")
                        .description("Production-grade issue tracker API"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH,
                                new SecurityScheme()
                                        .name(BEARER_AUTH)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}

/*{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI4MzA1NDQwNS02NTgyLTQ3OGMtYTcyZC1iOTM2ZGJiMTE4OTUiLCJlbWFpbCI6ImtyaXNoMUB0ZXN0LmNvbSIsImlzcyI6Imlzc3VlLXRyYWNrZXItbG9jYWwiLCJhdWQiOlsiaXNzdWUtdHJhY2tlci1hcGkiXSwiaWF0IjoxNzgyMTc3NzYyLCJleHAiOjE3ODIxNzg2NjJ9.3NKE6EhOyzXAxel-25kUbdMyh0ZZiKZ8zXRzPpurKcI",
  "refreshToken": "LbZqJO_LmobUm8fB-UciyjgW95RWAejDAkytapq24_4",
  "tokenType": "Bearer",
  "expiresIn": 900

  500 for deleting the admin
}*/