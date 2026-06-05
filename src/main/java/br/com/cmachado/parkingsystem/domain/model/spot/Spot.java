package br.com.cmachado.parkingsystem.domain.model.spot;

import br.com.cmachado.parkingsystem.domain.model.garage.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.spot.events.SpotOccupied;
import br.com.cmachado.parkingsystem.domain.model.spot.events.SpotRegistered;
import br.com.cmachado.parkingsystem.domain.model.spot.events.SpotReleased;
import br.com.cmachado.parkingsystem.domain.shared.AggregateRootBase;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Aggregate root representing a physical parking spot within a sector. The simulator's
 * original spot number is stored as {@code externalId}; occupancy is toggled via
 * {@link #occupy()} and {@link #release()}.
 */
@jakarta.persistence.Entity
@Table(name = "spot",
        uniqueConstraints = @UniqueConstraint(name = "uq_spot_sector_location", columnNames = {"sector_code", "lat", "lng"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Spot extends AggregateRootBase<Spot> {

    @EmbeddedId
    private SpotId id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "external_id")
    private Long externalId;

    @Embedded
    @AttributeOverride(name = "code", column = @Column(name = "sector_code", nullable = false))
    private SectorCode sectorCode;

    @Embedded
    private GeoLocation location;

    @Column(name = "occupied", nullable = false)
    private boolean occupied;

    private Spot(Long externalId, SectorCode sectorCode, GeoLocation location) {
        if (externalId == null) throw new IllegalArgumentException("ExternalId cannot be null");
        if (sectorCode == null) throw new IllegalArgumentException("SectorCode cannot be null");
        if (location == null) throw new IllegalArgumentException("Location cannot be null");

        this.id = SpotId.generate();
        this.externalId = externalId;
        this.sectorCode = sectorCode;
        this.location = location;
        this.occupied = false;
        registerEvent(new SpotRegistered(this));
    }

    public static Spot register(Long externalId, SectorCode sectorCode, GeoLocation location) {
        return new Spot(externalId, sectorCode, location);
    }

    public void occupy() {
        this.occupied = true;
        registerEvent(new SpotOccupied(this));
    }

    public void release() {
        this.occupied = false;
        registerEvent(new SpotReleased(this));
    }

    @Override
    public boolean sameIdentityAs(Spot other) {
        return other != null && this.id != null && this.id.sameValueAs(other.id);
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
