package com.example.shipping.service;

public class InvalidOrderTotalException extends IllegalArgumentException {

    public InvalidOrderTotalException(String message) {
        super(message);
    }
}
