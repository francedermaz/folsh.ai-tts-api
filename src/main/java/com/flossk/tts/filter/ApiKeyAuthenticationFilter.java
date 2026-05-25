package com.flossk.tts.filter;

import com.flossk.tts.entity.UserApiKey;
import com.flossk.tts.repository.UserApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    private final UserApiKeyRepository apiKeyRepository;
    
    public ApiKeyAuthenticationFilter(UserApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String apiKeyHeader = request.getHeader("X-API-KEY");
        
        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing X-API-KEY header");
            return;
        }
        
        try {
            UUID apiKey = UUID.fromString(apiKeyHeader);
            UserApiKey userApiKey = apiKeyRepository.findByKey(apiKey)
                .orElse(null);
            
            if (userApiKey == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid API key");
                return;
            }
            
            // Don't decrement tokens here - let the controller handle it based on actual text content
            // Just check if the API key is valid and has tokens available
            if (!userApiKey.hasTokens()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("API key has no remaining tokens");
                return;
            }
            
            request.setAttribute("userApiKey", userApiKey);
            
            filterChain.doFilter(request, response);
            
            // Note: Token usage history is now logged in the controllers with actual token counts
            
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid API key format");
        }
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Only filter /api/** paths, exclude Swagger and other paths
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            return true;
        }
        return !path.startsWith("/api/");
    }
}
