package com.example.shipping.controller;

import com.example.shipping.model.ShippingCost;
import com.example.shipping.model.ShippingRequest;
import com.example.shipping.service.InvalidWeightException;
import com.example.shipping.service.InvalidZoneException;
import com.example.shipping.service.ShippingCostService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShippingController {

    private final ShippingCostService service;

    public ShippingController(ShippingCostService service) {
        this.service = service;
    }

    @PostMapping("/api/shipping/calculate")
    public ShippingCost calculate(@RequestBody ShippingRequest request) {
        return service.calculate(request);
    }

    @ExceptionHandler({InvalidWeightException.class, InvalidZoneException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    void invalidRequest() {
    }
}
