package br.com.cmachado.parkingsystem.domain.model.parkingsession;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.violations.CantParkSessionException;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.violations.ParkingSpotOccupiedException;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpotId;
import br.com.cmachado.parkingsystem.domain.service.pricing.PricingElection;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.PricingStrategyType;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.events.VehicleEntered;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.events.VehicleExited;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.events.VehicleParked;
import br.com.cmachado.parkingsystem.domain.shared.AggregateRootBase;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Aggregate root tracking a single vehicle's stay, from entry through parking to exit.
 *
 * <p>State advances only through {@link #start}, {@link #parkOn} and {@link #exit}, which
 * guard the {@link ParkingSessionStatus} transitions. On exit a {@link VehicleExited} domain
 * event is registered so daily revenue is updated asynchronously.</p>
 */
@Entity
@Table(name = "parking_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParkingSession extends AggregateRootBase<ParkingSession> {

    @EmbeddedId
    private ParkingSessionId id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Embedded
    private LicensePlate licensePlate;

    @Embedded
    @AttributeOverride(name = "code", column = @Column(name = "sector_code"))
    private SectorCode sectorCode;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "spot_id", columnDefinition = "BINARY(16)"))
    private ParkingSpotId spotId;

    @Embedded
    private Period period;

    @Column(name = "parked_time")
    private LocalDateTime parkedTime;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "amount_charged"))
    private Money amountCharged;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ParkingSessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_election")
    private PricingElection pricingElection;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_strategy")
    private PricingStrategyType pricingStrategy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ParkingSession(LicensePlate licensePlate, LocalDateTime entryTime,
                           PricingElection pricingElection, PricingStrategyType pricingStrategy) {
        this.id = ParkingSessionId.generate();
        this.licensePlate = licensePlate;
        this.period = Period.start(entryTime);
        this.status = ParkingSessionStatus.ENTERED;
        this.pricingElection = pricingElection;
        this.pricingStrategy = pricingStrategy;
        registerEvent(new VehicleEntered(this));
    }

    /** Begins building a new {@code ENTERED} session for the given vehicle and entry time. */
    public static Builder start(LicensePlate plate, LocalDateTime entryTime) {
        return new Builder(plate, entryTime);
    }

    /**
     * Builds a {@link ParkingSession} in {@code ENTERED} status, recording the pricing election
     * mode and — when the strategy is elected at entry — the elected {@link PricingStrategyType}.
     */
    public static final class Builder {
        private final LicensePlate plate;
        private final LocalDateTime entryTime;
        private PricingElection election = PricingElection.AT_EXIT;
        private PricingStrategyType strategy;

        private Builder(LicensePlate plate, LocalDateTime entryTime) {
            this.plate = plate;
            this.entryTime = entryTime;
        }

        /** Sets the election mode that governs this session (defaults to {@code AT_EXIT}). */
        public Builder charging(PricingElection election) {
            this.election = election;
            return this;
        }

        /** Sets the strategy elected at entry; pass {@code null} when electing at exit. */
        public Builder strategy(PricingStrategyType strategy) {
            this.strategy = strategy;
            return this;
        }

        public ParkingSession build() {
            return new ParkingSession(plate, entryTime, election, strategy);
        }
    }

    public void parkOn(ParkingSpot parkingSpot) {
        if (this.status != ParkingSessionStatus.ENTERED)
            throw new CantParkSessionException("Session must be in ENTERED status to park");

        validateOccupied(parkingSpot);
        this.spotId = parkingSpot.getId();
        this.sectorCode = parkingSpot.getSectorCode();
        this.parkedTime = LocalDateTime.now();
        this.status = ParkingSessionStatus.PARKED;
        registerEvent(new VehicleParked(this));
    }

    private void validateOccupied(ParkingSpot parkingSpot) {
        if (parkingSpot.isOccupied())
            throw new ParkingSpotOccupiedException("Parking spot is already occupied");
    }

    /**
     * Records the exit time, the pricing strategy that was applied and the final charge, moves
     * the session to {@code EXITED} and registers a {@link VehicleExited} domain event.
     *
     * @throws IllegalStateException if the session has already exited
     */
    public void exit(LocalDateTime exitTime, Money amount, PricingStrategyType strategy) {
        if (this.status == ParkingSessionStatus.EXITED) {
            throw new IllegalStateException("Session has already exited");
        }
        this.period = this.period.end(exitTime);
        this.amountCharged = amount;
        this.pricingStrategy = strategy;
        this.status = ParkingSessionStatus.EXITED;
        registerEvent(new VehicleExited(this));
    }

    @Override
    public boolean sameIdentityAs(ParkingSession other) {
        return other != null && this.id != null && this.id.sameValueAs(other.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParkingSession that = (ParkingSession) o;
        return sameIdentityAs(that);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public boolean isParked() {
        return status == ParkingSessionStatus.PARKED && spotId != null;
    }

    public boolean hasNoCharge() {
        return this.amountCharged == null || this.amountCharged.isZero();
    }
}
