package br.com.cmachado.parkingsystem.infrastructure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for the garage simulator: fetches the garage configuration and registers
 * this app's webhook URL.
 */
@Component
public class SimulatorClient {

    private static final Logger logger = LoggerFactory.getLogger(SimulatorClient.class);

    private final RestClient restClient;

    @Value("${server.port:3003}")
    private int serverPort;

    public SimulatorClient(RestClient simulatorRestClient) {
        this.restClient = simulatorRestClient;
    }

    /**
     * Fetches the garage layout (sectors and spots) from {@code GET /garage}.
     *
     * @throws RuntimeException if the simulator cannot be reached or returns an error
     */
    public GarageResponse fetchGarageConfig() {
        logger.info("Fetching garage configuration from simulator...");
        try {
            return restClient.get()
                    .uri("/garage")
                    .retrieve()
                    .body(GarageResponse.class);
        } catch (Exception e) {
            logger.error("Failed to fetch garage config from simulator: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch garage config from simulator", e);
        }
    }

    /**
     * Registers this app's webhook URL with the simulator. Failures are logged but not
     * propagated, since the simulator may begin sending events on its own.
     */
    public void registerWebhook(String webhookUrl) {
        logger.info("Registering webhook: {}", webhookUrl);
        try {
            restClient.post()
                    .uri("/garage")
                    .header("Content-Type", "application/json")
                    .body("{\"webhook\":\"" + webhookUrl + "\"}")
                    .retrieve()
                    .toBodilessEntity();
            logger.info("Webhook registered successfully.");
        } catch (Exception e) {
            logger.warn("Webhook registration returned error (simulator may auto-start): {}", e.getMessage());
        }
    }
}
