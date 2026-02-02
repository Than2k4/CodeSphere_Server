package com.hcmute.codesphere_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration cho RestTemplate
 * Dùng để gọi Python ML API
 */
@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);  // 10 giây timeout khi connect
        factory.setReadTimeout(60000);     // 60 giây timeout khi đọc response (tăng để đợi OpenAI)
        
        return new RestTemplate(factory);
    }
}

