package com.jay.server.security;

import com.jay.server.config.AppServerProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Bearer token auth filter — equivalent to Rust's require_app_server_token middleware.
 * Applied to all routes except /healthz and /v1/chat/completions.
 */
@Component
public class AuthFilter implements Filter {

    private final AppServerProperties props;

    public AuthFilter(AppServerProperties props) {
        this.props = props;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        String path = request.getRequestURI();

        // Unprotected routes
        if (path.equals("/healthz") || path.startsWith("/v1/chat/completions")) {
            chain.doFilter(req, resp);
            return;
        }

        // Insecure mode — no auth required
        if (props.insecureNoAuth() || props.authToken() == null || props.authToken().isBlank()) {
            chain.doFilter(req, resp);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":{\"message\":\"app-server bearer token required\",\"status\":401}}");
            return;
        }

        String token = authHeader.substring(7).trim();
        if (!token.equals(props.authToken())) {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":{\"message\":\"app-server bearer token required\",\"status\":401}}");
            return;
        }

        chain.doFilter(req, resp);
    }
}
