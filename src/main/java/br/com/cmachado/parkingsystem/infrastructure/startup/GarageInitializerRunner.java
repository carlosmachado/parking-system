package br.com.cmachado.parkingsystem.infrastructure.startup;

import br.com.cmachado.parkingsystem.application.garage.GarageInitializerService;
import br.com.cmachado.parkingsystem.infrastructure.client.GarageResponse;
import br.com.cmachado.parkingsystem.infrastructure.client.SimulatorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * On startup, loads the garage layout from the simulator and registers this app's webhook.
 *
 * <p>The simulator may not be reachable the instant the app boots, so the fetch is retried
 * with a fixed delay up to a configurable number of attempts. A persistent failure is logged
 * rather than crashing the app, allowing a later manual/automatic retry.</p>
 */
@Component
public class GarageInitializerRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(GarageInitializerRunner.class);

    private final SimulatorClient simulatorClient;
    private final GarageInitializerService garageInitializerService;

    @Value("${server.port:3003}")
    private int serverPort;

    @Value("${webhook.host:localhost}")
    private String webhookHost;

    @Value("${garage.init.max-attempts:10}")
    private int maxAttempts;

    @Value("${garage.init.retry-delay-ms:3000}")
    private long retryDelayMs;

    public GarageInitializerRunner(SimulatorClient simulatorClient,
                                   GarageInitializerService garageInitializerService) {
        this.simulatorClient = simulatorClient;
        this.garageInitializerService = garageInitializerService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!initializeGarageWithRetry()) {
            logger.error("Garage initialization failed after {} attempts; the simulator may be "
                    + "unavailable. Vehicle events will fail until the garage is loaded.", maxAttempts);
            return;
        }

        String webhookUrl = "http://" + webhookHost + ":" + serverPort + "/webhook";
        simulatorClient.registerWebhook(webhookUrl);
    }

    /**
     * Fetches the garage configuration, retrying on failure until it succeeds or the attempt
     * budget is exhausted.
     *
     * @return {@code true} once the garage has been initialized, {@code false} otherwise
     */
    private boolean initializeGarageWithRetry() {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                GarageResponse config = simulatorClient.fetchGarageConfig();
                if (config != null) {
                    garageInitializerService.initializeGarage(config);
                    return true;
                }
                logger.warn("Received null garage configuration from simulator (attempt {}/{}).",
                        attempt, maxAttempts);
            } catch (Exception e) {
                logger.warn("Could not initialize garage from simulator (attempt {}/{}): {}",
                        attempt, maxAttempts, e.getMessage());
            }

            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }
}
