package br.com.cmachado.parkingsystem.domain.model.parkingsession;

import br.com.cmachado.parkingsystem.domain.shared.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Value object for a vehicle's license plate. Normalizes input to upper-case alphanumeric
 * characters so the same plate always compares equal regardless of formatting.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LicensePlate implements ValueObject<LicensePlate> {

    @Column(name = "license_plate", length = 20, nullable = false)
    private String plate;

    public LicensePlate(String plate) {
        Objects.requireNonNull(plate, "License plate cannot be null");
        if (plate.trim().isEmpty()) {
            throw new IllegalArgumentException("License plate cannot be blank");
        }
        this.plate = plate.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    @Override
    public boolean sameValueAs(LicensePlate other) {
        return other != null && this.plate.equals(other.plate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LicensePlate that = (LicensePlate) o;
        return sameValueAs(that);
    }

    @Override
    public int hashCode() {
        return plate.hashCode();
    }

    @Override
    public String toString() {
        return plate;
    }
}
