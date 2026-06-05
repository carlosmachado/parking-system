package br.com.cmachado.parkingsystem.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GarageResponse {

    @JsonProperty("garage")
    private List<SectorData> garage;

    @JsonProperty("spots")
    private List<SpotData> spots;

    @Data
    public static class SectorData {
        @JsonProperty("sector")
        private String sector;

        @JsonProperty("base_price")
        private Double basePrice;

        @JsonProperty("max_capacity")
        private Integer maxCapacity;

        @JsonProperty("open_hour")
        private String openHour;

        @JsonProperty("close_hour")
        private String closeHour;

        @JsonProperty("duration_limit_minutes")
        private Integer durationLimitMinutes;
    }

    @Data
    public static class SpotData {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("sector")
        private String sector;

        @JsonProperty("lat")
        private Double lat;

        @JsonProperty("lng")
        private Double lng;

        @JsonProperty("occupied")
        private Boolean occupied;
    }
}
