package com.example.shipping.json;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.shipping.model.DistanceZone;
import com.example.shipping.model.ShippingRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
class ShippingRequestJsonTest {

    @Autowired
    private JacksonTester<ShippingRequest> json;

    @ParameterizedTest(name = "The one where the zone \"{0}\" is accepted as {1}")
    @CsvSource({
            "EUROPEAN,      EUROPEAN",
            "european,      EUROPEAN",
            "International, INTERNATIONAL",
    })
    @DisplayName("Must accept zone names in any letter case")
    void zoneNamesAreMatchedIgnoringLetterCase(String zoneGiven, DistanceZone expectedZone) throws Exception {
        ShippingRequest request = json.parseObject("""
                { "weightKg": 3, "zone": "%s", "orderTotal": 50.00 }
                """.formatted(zoneGiven));

        assertThat(request.zone()).isEqualTo(expectedZone);
    }
}
