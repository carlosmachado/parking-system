package br.com.cmachado.parkingsystem.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class SimulatorClientConfig {

    @Value("${app.simulator.url}")
    private String simulatorUrl;

    @Value("${app.simulator.connect-timeout-ms:2000}")
    private long connectTimeoutMs;

    @Value("${app.simulator.read-timeout-ms:5000}")
    private long readTimeoutMs;

    @Bean
    public RestClient simulatorRestClient() {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(simulatorUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
