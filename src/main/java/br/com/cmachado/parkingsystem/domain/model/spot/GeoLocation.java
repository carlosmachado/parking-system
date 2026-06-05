package br.com.cmachado.parkingsystem.domain.model.spot;

import br.com.cmachado.parkingsystem.domain.shared.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Value object for a latitude/longitude pair. Provides a relative distance used to pick
 * the spot nearest to a vehicle's reported position.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GeoLocation implements ValueObject<GeoLocation> {

    @Column(name = "lat", nullable = false)
    private double lat;

    @Column(name = "lng", nullable = false)
    private double lng;

    public GeoLocation(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double calculateDistance(GeoLocation other) {
        if (other == null) return Double.MAX_VALUE;
        // Simple Euclidean distance or Haversine formula
        // Since garages are small, Euclidean is usually sufficient for relative distance
        double latDiff = this.lat - other.lat;
        double lngDiff = this.lng - other.lng;
        return Math.sqrt(latDiff * latDiff + lngDiff * lngDiff);
    }

    @Override
    public boolean sameValueAs(GeoLocation other) {
        return other != null && Double.compare(this.lat, other.lat) == 0 && Double.compare(this.lng, other.lng) == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeoLocation that = (GeoLocation) o;
        return sameValueAs(that);
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(lat);
        result = 31 * result + Double.hashCode(lng);
        return result;
    }
}
