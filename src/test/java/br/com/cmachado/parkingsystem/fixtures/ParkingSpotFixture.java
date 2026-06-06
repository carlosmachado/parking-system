package br.com.cmachado.parkingsystem.fixtures;

import br.com.cmachado.parkingsystem.domain.model.parkingspot.GeoLocation;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;

/** Fluent builder for {@link ParkingSpot} aggregates in tests; builds a registered, free spot. */
public final class ParkingSpotFixture {

    private long externalId = 1L;
    private String sectorCode = "A";
    private double lat = 10.0;
    private double lng = 10.0;

    private ParkingSpotFixture() {
    }

    public static ParkingSpotFixture aSpot() {
        return new ParkingSpotFixture();
    }

    public ParkingSpotFixture withExternalId(long externalId) {
        this.externalId = externalId;
        return this;
    }

    public ParkingSpotFixture withSector(String sectorCode) {
        this.sectorCode = sectorCode;
        return this;
    }

    public ParkingSpotFixture withLocation(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
        return this;
    }

    public ParkingSpot build() {
        return ParkingSpot.register(externalId, SectorCode.of(sectorCode), GeoLocation.of(lat, lng));
    }
}
