package br.com.cmachado.parkingsystem.domain.model.garage;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.garage.events.SectorCreated;
import br.com.cmachado.parkingsystem.domain.shared.AggregateRootBase;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Aggregate root representing a logical division of the garage's spot pool, with its own
 * base price and capacity. Sectors are organizational, not physical: all share the single
 * entrance gate group.
 */
@jakarta.persistence.Entity
@Table(name = "sector")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sector extends AggregateRootBase<Sector> {

    @EmbeddedId
    private SectorId id;

    @Embedded
    private SectorCode code;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "base_price", nullable = false))
    private Money basePrice;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    public Sector(SectorCode code, Money basePrice, Integer maxCapacity) {
        if (code == null) throw new IllegalArgumentException("SectorCode cannot be null");
        if (basePrice == null) throw new IllegalArgumentException("BasePrice cannot be null");
        if (maxCapacity == null || maxCapacity < 1) throw new IllegalArgumentException("MaxCapacity must be at least 1");

        this.id = SectorId.generate();
        this.code = code;
        this.basePrice = basePrice;
        this.maxCapacity = maxCapacity;
        registerEvent(new SectorCreated(this));
    }

    @Override
    public boolean sameIdentityAs(Sector other) {
        return other != null && this.id != null && this.id.sameValueAs(other.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sector sector = (Sector) o;
        return sameIdentityAs(sector);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
