package com.offerflow.api;

import com.offerflow.api.HealthController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OfferFlowApplicationTests {

    @Test
    void healthEndpointReturnsServiceStatus() throws Exception {
        var response = new HealthController().health();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("ok", response.getBody().get("status"));
        assertEquals("offerflow-api", response.getBody().get("service"));
    }
}
