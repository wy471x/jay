package com.jay.server.config;

import com.jay.core.AgentRuntime;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

@Configuration
@Import(AgentRuntime.class)
@EnableConfigurationProperties(AppServerProperties.class)
public class AppServerConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
            .requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory())
            .build();
    }
}
