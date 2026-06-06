package br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WebhookEventRequest {
    @JsonProperty("license_plate")
    private String licensePlate;
    
    @JsonProperty("event_type")
    private String eventType;
    
    @JsonProperty("entry_time")
    private String entryTime;
    
    @JsonProperty("exit_time")
    private String exitTime;

    @JsonProperty("lat")
    private Double lat;

    @JsonProperty("lng")
    private Double lng;
}
