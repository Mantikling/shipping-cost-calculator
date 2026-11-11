package com.example.shipping.service;

import com.example.shipping.model.Role;
import java.util.List;

public class ApiKeyService {

    private final List<String> userKeys;
    private final List<String> adminKeys;

    public ApiKeyService(List<String> userKeys, List<String> adminKeys) {
        this.userKeys = userKeys;
        this.adminKeys = adminKeys;
    }

    public Role authenticate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new MissingApiKeyException();
        }
        if (adminKeys.contains(apiKey)) {
            return Role.ADMIN;
        }
        if (userKeys.contains(apiKey)) {
            return Role.USER;
        }
        throw new InvalidApiKeyException();
    }
}
