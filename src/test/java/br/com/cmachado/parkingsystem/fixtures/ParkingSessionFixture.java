package br.com.cmachado.parkingsystem.fixtures;

import br.com.cmachado.parkingsystem.domain.model.parkingsession.LicensePlate;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.PricingElection;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.PricingStrategyType;

import java.time.LocalDateTime;

/** Fluent builder for {@link ParkingSession} aggregates in tests. */
public final class ParkingSessionFixture {

    private String plate = "ABC1234";
    private LocalDateTime entryTime = LocalDateTime.parse("2025-01-01T10:00:00");
    private PricingElection election = PricingElection.AT_EXIT;
    private PricingStrategyType strategy;
    private ParkingSpot parkOnSpot;

    private ParkingSessionFixture() {
    }

    public static ParkingSessionFixture aSession() {
        return new ParkingSessionFixture();
    }

    public ParkingSessionFixture withPlate(String plate) {
        this.plate = plate;
        return this;
    }

    public ParkingSessionFixture enteredAt(LocalDateTime entryTime) {
        this.entryTime = entryTime;
        return this;
    }

    public ParkingSessionFixture withElection(PricingElection election) {
        this.election = election;
        return this;
    }

    public ParkingSessionFixture withStrategy(PricingStrategyType strategy) {
        this.strategy = strategy;
        return this;
    }

    /** Parks the built session on the given spot, driving it to {@code PARKED}. */
    public ParkingSessionFixture parkedOn(ParkingSpot spot) {
        this.parkOnSpot = spot;
        return this;
    }

    public ParkingSession build() {
        ParkingSession session = ParkingSession.start(LicensePlate.of(plate), entryTime)
                .charging(election)
                .strategy(strategy)
                .build();
        if (parkOnSpot != null) {
            parkOnSpot.park(session);
        }
        return session;
    }
}
