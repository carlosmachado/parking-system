package br.com.cmachado.parkingsystem.domain.model.revenue;

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
 * Identity value object for the {@link DailyRevenue} aggregate. Backed by a ULID-generated
 * UUID stored as {@code BINARY(16)} in MySQL.
 */
@EqualsAndHashCode
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyRevenueId implements ValueObject<DailyRevenueId> {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID value;

    private DailyRevenueId(UUID value) {
        this.value = value;
    }

    public static DailyRevenueId generate() {
        return new DailyRevenueId(UlidCreator.getMonotonicUlid().toUuid());
    }

    public static DailyRevenueId of(UUID value) {
        return new DailyRevenueId(value);
    }

    @Override
    public boolean sameValueAs(DailyRevenueId other) {
        return other != null && Objects.equals(this.value, other.value);
    }

    @Override
    public String toString() {
        return value != null ? value.toString() : "null";
    }
}
