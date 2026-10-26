package edu.campus.auction.web;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    private final String[] origins;
    public WebConfiguration(@Value("${auction.allowed-origins}") String[] origins) { this.origins = origins; }
    @Bean
    public Clock clock() { return Clock.systemUTC(); }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(origins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization").maxAge(3600);
    }
}
