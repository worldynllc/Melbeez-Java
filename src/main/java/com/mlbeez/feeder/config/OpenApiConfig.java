package com.mlbeez.feeder.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        SecurityScheme securityScheme = new SecurityScheme()
                .name("Bearer Authentication")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("Bearer Authentication");

        return new OpenAPI()
                .addServersItem(new Server().url("https://preprodjavaapi.melbeez.com"))
                .info(new Info()
                        .title("Melbeez-Java API")
                        .version("v1")
                        .description("API documentation for Melbeez-Java")
                        .termsOfService("https://your-terms-of-service.url")
                        .contact(new Contact()
                                .name("Developer Name")
                                .url("https://contact-url.com")
                                .email("developer@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                        .components(new Components().addSecuritySchemes("Bearer Authentication", securityScheme))
                        .addSecurityItem(securityRequirement);
    }
}