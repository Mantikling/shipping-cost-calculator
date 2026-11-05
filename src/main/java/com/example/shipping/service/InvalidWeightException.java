package com.example.shipping.service;

public class InvalidWeightException extends IllegalArgumentException {

    public InvalidWeightException(String message) {
        super(message);
    }
}
