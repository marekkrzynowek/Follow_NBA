package com.nba.standings.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for NBA API client
 */
@Configuration
public class NBAApiConfig {
    
    @Value("${nba.api.base-url}")
    private String baseUrl;
    
    @Value("${nba.api.key}")
    private String apiKey;
    
    /**
     * Creates a WebClient bean configured for NBA API calls
     * with base URL and authentication headers
     */
    @Bean
    public WebClient nbaWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", apiKey)
                .build();
    }
}
