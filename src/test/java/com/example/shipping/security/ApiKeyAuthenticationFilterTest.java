package com.example.shipping.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.example.shipping.service.ApiKeyService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiKeyAuthenticationFilterTest {

    private final ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(
            new ApiKeyService(List.of("user-key"), List.of("admin-key")));

    @Test
    @DisplayName("The one where a request with no X-API-Key header is rejected with a 401 \"Missing API key\" JSON error and goes no further")
    void requestWithNoApiKeyHeaderIsRejectedAsUnauthorized() throws Exception {
        Exchange exchange = send(null);

        exchange.assertRejectedAsUnauthorized("Missing API key");
    }

    @Test
    @DisplayName("The one where a request with the key \"not-a-real-key\" is rejected with a 401 \"Invalid API key\" JSON error and goes no further")
    void requestWithInvalidApiKeyIsRejectedAsUnauthorized() throws Exception {
        Exchange exchange = send("not-a-real-key");

        exchange.assertRejectedAsUnauthorized("Invalid API key");
    }

    private Exchange send(String apiKey) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/shipping/calculate");
        if (apiKey != null) {
            request.addHeader("X-API-Key", apiKey);
        }
        Exchange exchange = new Exchange(new MockHttpServletResponse(), new MockFilterChain());
        assertThatNoException().isThrownBy(
                () -> filter.doFilter(request, exchange.response(), exchange.chain()));
        return exchange;
    }

    private record Exchange(MockHttpServletResponse response, MockFilterChain chain) {

        void assertRejectedAsUnauthorized(String expectedMessage) throws Exception {
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentType()).startsWith("application/json");
            assertThat(response.getContentAsString()).isEqualTo(
                    "{\"error\":\"Unauthorized\",\"message\":\"" + expectedMessage + "\"}");
            assertThat(chain.getRequest()).isNull();
        }
    }
}
