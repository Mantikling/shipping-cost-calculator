package com.example.shipping.security;

import com.example.shipping.service.ApiKeyService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableConfigurationProperties(ApiKeyProperties.class)
public class SecurityConfig {

    @Bean
    ApiKeyService apiKeyService(ApiKeyProperties properties) {
        return new ApiKeyService(properties.user(), properties.admin());
    }

    /** API documentation is public: this chain never looks at the X-API-Key header. */
    @Bean
    @Order(1)
    SecurityFilterChain documentationFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiFilterChain(HttpSecurity http, ApiKeyService apiKeyService) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .exceptionHandling(handling -> handling.accessDeniedHandler(
                        (request, response, denied) -> ApiErrorWriter.write(response,
                                HttpServletResponse.SC_FORBIDDEN, "Forbidden", "Insufficient role")))
                .addFilterBefore(new ApiKeyAuthenticationFilter(apiKeyService),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
