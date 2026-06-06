package br.com.cmachado.parkingsystem.fixtures;

import br.com.cmachado.parkingsystem.infrastructure.client.GarageResponse;

import java.util.ArrayList;
import java.util.List;

/** Fluent builders for the simulator's {@link GarageResponse} and its nested data in tests. */
public final class GarageResponseFixture {

    private final List<GarageResponse.SectorData> sectors = new ArrayList<>();
    private final List<GarageResponse.SpotData> spots = new ArrayList<>();

    private GarageResponseFixture() {
    }

    public static GarageResponseFixture aGarage() {
        return new GarageResponseFixture();
    }

    public GarageResponseFixture withSector(GarageResponse.SectorData sector) {
        this.sectors.add(sector);
        return this;
    }

    public GarageResponseFixture withSpot(GarageResponse.SpotData spot) {
        this.spots.add(spot);
        return this;
    }

    public GarageResponse build() {
        GarageResponse response = new GarageResponse();
        response.setGarage(sectors.isEmpty() ? null : sectors);
        response.setSpots(spots.isEmpty() ? null : spots);
        return response;
    }

    public static SectorDataFixture aSectorData() {
        return new SectorDataFixture();
    }

    public static SpotDataFixture aSpotData() {
        return new SpotDataFixture();
    }

    /** Fluent builder for {@link GarageResponse.SectorData}. */
    public static final class SectorDataFixture {
        private String sector = "A";
        private Double basePrice = 10.0;
        private Integer maxCapacity = 10;
        private String openHour = "08:00";
        private String closeHour = "20:00";
        private Integer durationLimitMinutes = 240;

        public SectorDataFixture withCode(String sector) {
            this.sector = sector;
            return this;
        }

        public SectorDataFixture withBasePrice(Double basePrice) {
            this.basePrice = basePrice;
            return this;
        }

        public SectorDataFixture withMaxCapacity(Integer maxCapacity) {
            this.maxCapacity = maxCapacity;
            return this;
        }

        public SectorDataFixture withHours(String openHour, String closeHour) {
            this.openHour = openHour;
            this.closeHour = closeHour;
            return this;
        }

        public SectorDataFixture withDurationLimitMinutes(Integer durationLimitMinutes) {
            this.durationLimitMinutes = durationLimitMinutes;
            return this;
        }

        public GarageResponse.SectorData build() {
            GarageResponse.SectorData data = new GarageResponse.SectorData();
            data.setSector(sector);
            data.setBasePrice(basePrice);
            data.setMaxCapacity(maxCapacity);
            data.setOpenHour(openHour);
            data.setCloseHour(closeHour);
            data.setDurationLimitMinutes(durationLimitMinutes);
            return data;
        }
    }

    /** Fluent builder for {@link GarageResponse.SpotData}. */
    public static final class SpotDataFixture {
        private Long id = 1L;
        private String sector = "A";
        private Double lat = -23.0;
        private Double lng = -46.0;

        public SpotDataFixture withId(Long id) {
            this.id = id;
            return this;
        }

        public SpotDataFixture withSector(String sector) {
            this.sector = sector;
            return this;
        }

        public SpotDataFixture withLocation(Double lat, Double lng) {
            this.lat = lat;
            this.lng = lng;
            return this;
        }

        public GarageResponse.SpotData build() {
            GarageResponse.SpotData data = new GarageResponse.SpotData();
            data.setId(id);
            data.setSector(sector);
            data.setLat(lat);
            data.setLng(lng);
            return data;
        }
    }
}
