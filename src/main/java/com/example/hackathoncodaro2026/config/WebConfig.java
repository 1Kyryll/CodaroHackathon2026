package com.example.hackathoncodaro2026.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, Long.class, source -> {
            if (source == null || source.isBlank()) {
                return null;
            }
            return Long.valueOf(source.trim());
        });
    }
}
