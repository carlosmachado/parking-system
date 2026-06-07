package br.com.cmachado.parkingsystem.domain.model.parkingspot;

import br.com.cmachado.parkingsystem.domain.model.parkingsession.violations.CantParkSessionException;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionStatus;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.events.ParkingSpotRegistered;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.events.ParkingSpotOccupied;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.events.ParkingSpotReleased;
import br.com.cmachado.parkingsystem.domain.shared.AggregateRootBase;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Aggregate root representing a physical parking spot within a sector. The simulator's
 * original spot number is stored as {@code externalId};
 */
@Entity
@Table(name = "parking_spot",
        uniqueConstraints = @UniqueConstraint(name = "uq_spot_sector_location", columnNames = {"sector_code", "lat", "lng"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParkingSpot extends AggregateRootBase<ParkingSpot> {

    @EmbeddedId
    private ParkingSpotId id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "external_id")
    private Long externalId;

    @NotNull(message = "Sector code is required")
    @Embedded
    @AttributeOverride(name = "code", column = @Column(name = "sector_code", nullable = false))
    private SectorCode sectorCode;

    @Embedded
    private GeoLocation location;

    @Column(name = "occupied", nullable = false)
    private boolean occupied;

    private ParkingSpot(Long externalId, SectorCode sectorCode, GeoLocation location) {
        Objects.requireNonNull(externalId, "ExternalId cannot be null");
        Objects.requireNonNull(sectorCode, "SectorCode cannot be null");
        Objects.requireNonNull(location, "Location cannot be null");

        this.id = ParkingSpotId.generate();
        this.externalId = externalId;
        this.sectorCode = sectorCode;
        this.location = location;
        this.occupied = false;
        registerEvent(new ParkingSpotRegistered(this));
    }

    public static ParkingSpot register(Long externalId, SectorCode sectorCode, GeoLocation location) {
        return new ParkingSpot(externalId, sectorCode, location);
    }

    /** Updates sector and location to match the latest data from the simulator. */
    public void updateLocation(SectorCode sectorCode, GeoLocation location) {
        this.sectorCode = sectorCode;
        this.location = location;
    }

    public void park(ParkingSession session) {
        throwIfNotEnteredStatus(session);
        session.parkOn(this);
        this.occupied = true;
        registerEvent(new ParkingSpotOccupied(this));
    }

    public void release() {
        this.occupied = false;
        registerEvent(new ParkingSpotReleased(this));
    }

    @Override
    public boolean sameIdentityAs(ParkingSpot other) {
        return other != null && this.id != null && this.id.sameValueAs(other.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParkingSpot that = (ParkingSpot) o;
        return sameIdentityAs(that);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    private void throwIfNotEnteredStatus(ParkingSession session) {
        if (session.getStatus() != ParkingSessionStatus.ENTERED)
            throw new CantParkSessionException("Parking session must be ENTERED before it can occupy a spot");
    }
}
