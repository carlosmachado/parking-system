package br.com.cmachado.parkingsystem.domain.model.spot;

import br.com.cmachado.parkingsystem.domain.model.garage.SectorCode;
import br.com.cmachado.parkingsystem.domain.shared.Entity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A physical parking spot within a sector. Its id and location come from the simulator's
 * garage configuration; occupancy is toggled via {@link #occupy()} and {@link #release()}.
 */
@jakarta.persistence.Entity
@Table(name = "spot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Spot implements Entity<Spot> {

    @Id
    private Long id; // Not auto-generated, comes from simulator

    @Embedded
    @AttributeOverride(name = "code", column = @Column(name = "sector_code", nullable = false))
    private SectorCode sectorCode;

    @Embedded
    private GeoLocation location;

    @Column(name = "occupied", nullable = false)
    private boolean occupied;

    public Spot(Long id, SectorCode sectorCode, GeoLocation location) {
        if (id == null) throw new IllegalArgumentException("Id cannot be null");
        if (sectorCode == null) throw new IllegalArgumentException("SectorCode cannot be null");
        if (location == null) throw new IllegalArgumentException("Location cannot be null");
        
        this.id = id;
        this.sectorCode = sectorCode;
        this.location = location;
        this.occupied = false;
    }

    public void occupy() {
        this.occupied = true;
    }

    public void release() {
        this.occupied = false;
    }

    @Override
    public boolean sameIdentityAs(Spot other) {
        return other != null && this.id != null && this.id.equals(other.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Spot spot = (Spot) o;
        return sameIdentityAs(spot);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
