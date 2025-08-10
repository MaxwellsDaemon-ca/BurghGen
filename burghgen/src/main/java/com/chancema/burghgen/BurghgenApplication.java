package com.chancema.burghgen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Main entry point for the BurghGen Spring Boot application.
 * 
 * This class initializes the application context and enables a limited CORS configuration
 * to allow frontend development access from a specific origin (e.g., localhost:5173).
 */
@SpringBootApplication
public class BurghgenApplication {

    /**
     * Launches the Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(BurghgenApplication.class, args);
    }

    /**
     * Configures Cross-Origin Resource Sharing (CORS) to allow requests from the frontend.
     * 
     * This setup is useful for local development (e.g., Vite or React dev server on port 5173).
     * 
     * @return a WebMvcConfigurer with custom CORS mappings
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("https://MaxwellsDaemon-ca.github.io", "https://MaxwellsDaemon-ca.github.io/BurghGen");
            }
        };
    }
}
