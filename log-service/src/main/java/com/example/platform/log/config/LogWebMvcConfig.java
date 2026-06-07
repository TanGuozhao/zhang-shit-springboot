package com.example.platform.log.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class LogWebMvcConfig implements WebMvcConfigurer {

    private final LogServiceProperties properties;

    public LogWebMvcConfig(LogServiceProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(properties.export().directory()).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/downloads/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
