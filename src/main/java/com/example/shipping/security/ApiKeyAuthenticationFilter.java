package com.example.shipping.security;

import com.example.shipping.model.Role;
import com.example.shipping.service.ApiKeyService;
import com.example.shipping.service.InvalidApiKeyException;
import com.example.shipping.service.MissingApiKeyException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        Role role;
        try {
            role = apiKeyService.authenticate(request.getHeader("X-API-Key"));
        } catch (MissingApiKeyException | InvalidApiKeyException e) {
            ApiErrorWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized", e.getMessage());
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                role.name(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
        chain.doFilter(request, response);
    }
}
