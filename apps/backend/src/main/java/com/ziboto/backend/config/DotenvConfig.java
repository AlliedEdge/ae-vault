package com.ziboto.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class to load environment variables from .env file.
 * This ensures that environment variables defined in .env are available
 * to the Spring application context.
 */
@Slf4j
@Component
public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            Path envFile = Paths.get(".env");
            
            if (Files.exists(envFile)) {
                Map<String, Object> envProperties = new HashMap<>();
                
                Files.lines(envFile)
                    .filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("#"))
                    .forEach(line -> {
                        int separatorIndex = line.indexOf('=');
                        if (separatorIndex > 0) {
                            String key = line.substring(0, separatorIndex).trim();
                            String value = line.substring(separatorIndex + 1).trim();
                            
                            // Remove quotes if present
                            if (value.startsWith("\"") && value.endsWith("\"")) {
                                value = value.substring(1, value.length() - 1);
                            } else if (value.startsWith("'") && value.endsWith("'")) {
                                value = value.substring(1, value.length() - 1);
                            }
                            
                            // Remove inline comments
                            int commentIndex = value.indexOf('#');
                            if (commentIndex > 0) {
                                value = value.substring(0, commentIndex).trim();
                            }
                            
                            envProperties.put(key, value);
                        }
                    });
                
                ConfigurableEnvironment environment = applicationContext.getEnvironment();
                environment.getPropertySources().addFirst(new MapPropertySource("dotenv", envProperties));
                
                log.info("Successfully loaded {} environment variables from .env file", envProperties.size());
            } else {
                log.warn(".env file not found in project root. Using default configuration or system environment variables.");
            }
        } catch (IOException e) {
            log.error("Failed to load .env file: {}", e.getMessage());
        }
    }
}
