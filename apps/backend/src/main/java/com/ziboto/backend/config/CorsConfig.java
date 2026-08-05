package com.ziboto.backend.config;

import com.ziboto.backend.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {
    
    private final AppProperties appProperties;
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        AppProperties.Security.Cors corsProps = appProperties.getSecurity().getCors();
        
        configuration.setAllowedOrigins(
                Arrays.asList(corsProps.getAllowedOrigins().split(","))
        );
        configuration.setAllowedMethods(
                Arrays.asList(corsProps.getAllowedMethods().split(","))
        );
        configuration.setAllowedHeaders(
                List.of(corsProps.getAllowedHeaders())
        );
        configuration.setAllowCredentials(corsProps.getAllowCredentials());
        configuration.setMaxAge(corsProps.getMaxAge());
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
