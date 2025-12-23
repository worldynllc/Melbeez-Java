package com.mlbeez.feeder.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

@Configuration
public class AppConfig {

    @Bean
    public Supplier<String> uuidGenerator() {
        return () -> java.util.UUID.randomUUID().toString();
    }
}