package com.example.shipping.controller;

import com.example.shipping.model.RateCard;
import com.example.shipping.service.RateCardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

    private final RateCardService rateCardService;

    public AdminController(RateCardService rateCardService) {
        this.rateCardService = rateCardService;
    }

    @GetMapping("/api/admin/rates")
    public RateCard rates() {
        return rateCardService.rateCard();
    }
}
