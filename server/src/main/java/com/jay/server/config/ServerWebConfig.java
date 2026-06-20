package com.jay.server.config;

import com.jay.server.security.AuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class ServerWebConfig implements WebMvcConfigurer {

    private final AppServerProperties props;

    public ServerWebConfig(AppServerProperties props) {
        this.props = props;
    }

    @Bean
    public FilterRegistrationBean<AuthFilter> authFilterRegistration(AuthFilter filter) {
        var reg = new FilterRegistrationBean<>(filter);
        reg.addUrlPatterns("/thread", "/app", "/prompt", "/tool", "/jobs", "/mcp/startup",
            "/thread/*", "/app/*", "/prompt/*", "/tool/*", "/jobs/*", "/mcp/*");
        reg.setOrder(1);
        return reg;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = props.corsOrigins().isEmpty()
            ? AppServerProperties.defaultCorsOrigins()
            : props.corsOrigins();

        registry.addMapping("/**")
            .allowedOrigins(origins.toArray(new String[0]))
            .allowedMethods("GET", "POST", "OPTIONS")
            .allowedHeaders("Authorization", "Content-Type");
    }
}
