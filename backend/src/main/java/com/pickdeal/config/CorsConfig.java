package com.pickdeal.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer(
            @Value("${pickdeal.cors.allowed-origins:}") String allowedOrigins,
            @Value("${pickdeal.cors.allow-localhost:true}") boolean allowLocalhost) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).toArray(String[]::new);
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                var registration = registry.addMapping("/api/**")
                        .allowedOrigins(origins)
                        .allowedMethods("GET", "HEAD", "POST", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(false);
                // Compose에서는 끄고, 로컬 개발에서만 가변 dev 포트를 허용한다.
                if (allowLocalhost) {
                    registration.allowedOriginPatterns("http://localhost:[*]");
                }
            }
        };
    }
}
