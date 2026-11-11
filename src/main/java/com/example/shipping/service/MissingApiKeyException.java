package com.example.shipping.service;

public class MissingApiKeyException extends RuntimeException {

    public MissingApiKeyException() {
        super("Missing API key");
    }
}
