package br.com.cmachado.parkingsystem.infrastructure.client;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

class SimulatorClientTest {

    @Test
    void fetchGarageConfigWrapsRestClientFailure() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://simulator");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://simulator/garage")).andRespond(withServerError());
        SimulatorClient client = new SimulatorClient(builder.build());

        assertThrows(SimulatorClientException.class, client::fetchGarageConfig,
                "simulator fetch failures must be wrapped in typed infrastructure exception");

        server.verify();
    }
}
