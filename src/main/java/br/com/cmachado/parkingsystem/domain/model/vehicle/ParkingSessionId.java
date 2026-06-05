package br.com.cmachado.parkingsystem.domain.model.vehicle;

import br.com.cmachado.parkingsystem.domain.shared.ValueObject;
import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity value object for the {@link ParkingSession} aggregate. Backed by a ULID-generated
 * UUID stored as {@code BINARY(16)} in MySQL.
 */
@EqualsAndHashCode
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParkingSessionId implements ValueObject<ParkingSessionId> {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID value;

    private ParkingSessionId(UUID value) {
        this.value = value;
    }

    public static ParkingSessionId generate() {
        return new ParkingSessionId(UlidCreator.getMonotonicUlid().toUuid());
    }

    public static ParkingSessionId of(UUID value) {
        return new ParkingSessionId(value);
    }

    @Override
    public boolean sameValueAs(ParkingSessionId other) {
        return other != null && Objects.equals(this.value, other.value);
    }

    @Override
    public String toString() {
        return value != null ? value.toString() : "null";
    }
}
