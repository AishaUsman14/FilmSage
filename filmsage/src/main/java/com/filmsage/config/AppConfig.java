package com.filmsage.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class AppConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    @Bean
    public RestTemplate restTemplate() {
        // Create a factory with increased timeout values
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 30 seconds
        factory.setReadTimeout(120000);   // 2 minutes for LLM responses which can be slow
        
        // Use buffering factory to allow logging request/response bodies
        ClientHttpRequestFactory bufferingFactory = new BufferingClientHttpRequestFactory(factory);
        
        // Create RestTemplate with the factory
        RestTemplate restTemplate = new RestTemplateBuilder()
                .requestFactory(() -> bufferingFactory)
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofMinutes(2))
                .build();
        
        // Add a logging interceptor
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add((request, body, execution) -> {
            logger.debug("Making request to URL: {}", request.getURI());
            logger.debug("Request method: {}", request.getMethod());
            logger.debug("Request headers: {}", request.getHeaders());
            
            // Execute the request
            try {
                return execution.execute(request, body);
            } catch (Exception e) {
                logger.error("Error during REST call: {}", e.getMessage(), e);
                throw e;
            }
        });
        
        restTemplate.setInterceptors(interceptors);
        
        logger.info("RestTemplate configured with increased timeouts: connect=30s, read=120s");
        return restTemplate;
    }
} 