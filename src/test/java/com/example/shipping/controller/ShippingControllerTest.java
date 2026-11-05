package com.example.shipping.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.example.shipping.model.ShippingCost;
import com.example.shipping.model.ShippingRequest;
import com.example.shipping.service.InvalidWeightException;
import com.example.shipping.service.ShippingCostService;
import java.math.BigDecimal;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest(ShippingController.class)
class ShippingControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ShippingCostService service;

    @Test
    @DisplayName("The one where the calculate endpoint returns the cost breakdown as JSON")
    void calculateReturnsCostBreakdownAsJson() {
        given(service.calculate(any(ShippingRequest.class))).willReturn(
                new ShippingCost(new BigDecimal("2.99"),
                        new ShippingCost.Breakdown(
                                new BigDecimal("2.99"), new BigDecimal("1.0"), new BigDecimal("2.99"), false)));

        MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "weightKg": 0.5, "zone": "DOMESTIC", "orderTotal": 50.00 }
                        """)
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson()
                .extractingPath("$.breakdown.baseRate")
                .convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
                .isEqualByComparingTo("2.99");
    }

    @Test
    @DisplayName("The one where an invalid parcel weight is rejected with a 400")
    void invalidWeightIsRejectedWithBadRequest() {
        given(service.calculate(any(ShippingRequest.class)))
                .willThrow(new InvalidWeightException("Parcel weight must be greater than 0kg"));

        MvcTestResult result = mvc.post().uri("/api/shipping/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "weightKg": 0, "zone": "DOMESTIC", "orderTotal": 50.00 }
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }
}
