package br.com.cmachado.parkingsystem.domain.model.garage;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.shared.Entity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A logical division of the garage's spot pool, with its own base price and capacity.
 * Sectors are organizational, not physical: all share the single entrance gate group.
 */
@jakarta.persistence.Entity
@Table(name = "sector")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sector implements Entity<Sector> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
        
        this.code = code;
        this.basePrice = basePrice;
        this.maxCapacity = maxCapacity;
    }

    @Override
    public boolean sameIdentityAs(Sector other) {
        return other != null && this.id != null && this.id.equals(other.id);
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
