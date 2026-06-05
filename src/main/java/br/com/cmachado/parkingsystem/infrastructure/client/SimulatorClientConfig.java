package br.com.cmachado.parkingsystem.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class SimulatorClientConfig {

    @Value("${simulator.url}")
    private String simulatorUrl;

    @Bean
    public RestClient simulatorRestClient() {
        return RestClient.builder()
                .baseUrl(simulatorUrl)
                .build();
    }
}
