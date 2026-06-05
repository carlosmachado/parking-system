package br.com.cmachado.parkingsystem.domain.model.garage;

import br.com.cmachado.parkingsystem.domain.shared.ValueObject;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Value object for the garage occupancy fraction (0.0–1.0) captured at a point in time.
 * Drives selection of the dynamic pricing strategy.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OccupancyRate implements ValueObject<OccupancyRate> {

    private double rate;
    private LocalDateTime timestamp;

    public OccupancyRate(double rate, LocalDateTime timestamp) {
        if (rate < 0.0 || rate > 1.0) {
            throw new IllegalArgumentException("Occupancy rate must be between 0.0 and 1.0");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        this.rate = rate;
        this.timestamp = timestamp;
    }

    @Override
    public boolean sameValueAs(OccupancyRate other) {
        if (other == null) return false;
        return Double.compare(this.rate, other.rate) == 0 && this.timestamp.equals(other.timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OccupancyRate that = (OccupancyRate) o;
        return sameValueAs(that);
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(rate);
        result = 31 * result + timestamp.hashCode();
        return result;
    }
}
