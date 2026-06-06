package br.com.cmachado.parkingsystem.fixtures;

import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookEventRequest;

import java.time.LocalDateTime;

/** Fluent builder for {@link WebhookEventRequest} payloads in tests. */
public final class WebhookEventFixture {

    private String plate = "ABC1234";
    private final String eventType;
    private String entryTime;
    private String exitTime;
    private Double lat;
    private Double lng;

    private WebhookEventFixture(String eventType) {
        this.eventType = eventType;
    }

    public static WebhookEventFixture anEntry() {
        return new WebhookEventFixture("ENTRY");
    }

    public static WebhookEventFixture aParked() {
        return new WebhookEventFixture("PARKED");
    }

    public static WebhookEventFixture anExit() {
        return new WebhookEventFixture("EXIT");
    }

    /** Builds a request with the given raw event type, for invalid/unknown-type cases. */
    public static WebhookEventFixture ofType(String eventType) {
        return new WebhookEventFixture(eventType);
    }

    public WebhookEventFixture withPlate(String plate) {
        this.plate = plate;
        return this;
    }

    public WebhookEventFixture at(LocalDateTime time) {
        if ("EXIT".equals(eventType)) {
            this.exitTime = time.toString();
        } else {
            this.entryTime = time.toString();
        }
        return this;
    }

    public WebhookEventFixture atLocation(Double lat, Double lng) {
        this.lat = lat;
        this.lng = lng;
        return this;
    }

    public WebhookEventRequest build() {
        WebhookEventRequest request = new WebhookEventRequest();
        request.setLicensePlate(plate);
        request.setEventType(eventType);
        request.setEntryTime(entryTime);
        request.setExitTime(exitTime);
        request.setLat(lat);
        request.setLng(lng);
        return request;
    }
}
