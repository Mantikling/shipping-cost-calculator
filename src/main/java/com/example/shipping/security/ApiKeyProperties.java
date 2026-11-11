package com.example.shipping.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * API keys per role, bound from api.keys.user and api.keys.admin. Both are required:
 * an unset, empty, blank or unresolved (still "${...}") value stops startup, so no
 * placeholder text can ever be accepted as a key.
 */
@ConfigurationProperties(prefix = "api.keys")
public record ApiKeyProperties(List<String> user, List<String> admin) {

    public ApiKeyProperties {
        requireUsableKeys("api.keys.user", user);
        requireUsableKeys("api.keys.admin", admin);
    }

    private static void requireUsableKeys(String property, List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            throw new IllegalStateException(property + " is not set: export the matching environment variable");
        }
        for (String key : keys) {
            if (key == null || key.isBlank() || key.contains("${")) {
                throw new IllegalStateException(property + " has an unusable key: check the matching environment variable");
            }
        }
    }
}
