package com.example.shipping.service;

public class InvalidZoneException extends IllegalArgumentException {

    public InvalidZoneException(String message) {
        super(message);
    }
}
