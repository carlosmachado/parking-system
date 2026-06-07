package br.com.cmachado.parkingsystem.domain.model.parkingsession;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.events.VehicleEntered;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.events.VehicleExited;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.events.VehicleParked;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.violations.CantParkSessionException;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.violations.ParkingSessionAlreadyExitedException;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.service.pricing.strategy.PricingStrategyType;
import br.com.cmachado.parkingsystem.fixtures.ParkingSessionFixture;
import br.com.cmachado.parkingsystem.fixtures.ParkingSpotFixture;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static br.com.cmachado.parkingsystem.support.DomainEventAssertions.assertHasEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParkingSessionTest {

    private static final LocalDateTime ENTRY = LocalDateTime.parse("2025-01-01T10:00:00");
    private static final LocalDateTime EXIT = LocalDateTime.parse("2025-01-01T12:00:00");

    @Test
    void startBuildsSessionInEnteredStatusRecordsElectionAndRegistersEvent() {
        // arrange / act
        ParkingSession session = ParkingSessionFixture.aSession().enteredAt(ENTRY)
                .withElection(PricingElection.AT_ENTRY)
                .withStrategy(PricingStrategyType.SURCHARGE_10)
                .build();

        // assert
        assertEquals(ParkingSessionStatus.ENTERED, session.getStatus(), "new session must be ENTERED");
        assertEquals(PricingElection.AT_ENTRY, session.getPricingElection(), "election mode must be recorded");
        assertEquals(PricingStrategyType.SURCHARGE_10, session.getPricingStrategy(),
                "strategy elected at entry must be stored");
        assertHasEvent(session, VehicleEntered.class);
    }

    @Test
    void parkOnMovesToParkedAndCapturesSpotDetailsAndRegistersEvent() {
        // arrange
        ParkingSession session = ParkingSessionFixture.aSession().enteredAt(ENTRY).build();
        ParkingSpot spot = ParkingSpotFixture.aSpot().withSector("A").build();

        // act
        session.parkOn(spot);

        // assert
        assertEquals(ParkingSessionStatus.PARKED, session.getStatus(), "parked session must be PARKED");
        assertEquals(spot.getId(), session.getSpotId(), "session must capture the spot id");
        assertEquals(SectorCode.of("A"), session.getSectorCode(), "session must capture the sector code");
        assertNotNull(session.getParkedTime(), "parked time must be set");
        assertHasEvent(session, VehicleParked.class);
    }

    @Test
    void exitMovesToExitedStoresChargeAndRegistersEvent() {
        // arrange
        ParkingSpot spot = ParkingSpotFixture.aSpot().build();
        ParkingSession session = ParkingSessionFixture.aSession().enteredAt(ENTRY).parkedOn(spot).build();

        // act
        session.exit(EXIT, Money.of("22.00"), PricingStrategyType.SURCHARGE_10);

        // assert
        assertEquals(ParkingSessionStatus.EXITED, session.getStatus(), "exited session must be EXITED");
        assertEquals(Money.of("22.00"), session.getAmountCharged(), "charge must be stored");
        assertEquals(PricingStrategyType.SURCHARGE_10, session.getPricingStrategy(),
                "applied strategy must be stored at exit");
        assertHasEvent(session, VehicleExited.class);
    }

    @Test
    void parkOnRejectsSessionNotInEnteredStatus() {
        // arrange
        ParkingSpot spot = ParkingSpotFixture.aSpot().build();
        ParkingSession session = ParkingSessionFixture.aSession().enteredAt(ENTRY).parkedOn(spot).build();
        ParkingSpot anotherSpot = ParkingSpotFixture.aSpot().withExternalId(2L).build();

        // act / assert
        assertThrows(CantParkSessionException.class, () -> session.parkOn(anotherSpot),
                "must not park a session that is already PARKED");
    }

    @Test
    void buildRejectsAtEntryWithoutStrategy() {
        // act / assert
        assertThrows(IllegalStateException.class,
                () -> ParkingSession.start(LicensePlate.of("BAD0001"), ENTRY)
                        .charging(PricingElection.AT_ENTRY)
                        .build(),
                "AT_ENTRY without strategy must be rejected");
    }

    @Test
    void exitRejectsAlreadyExitedSession() {
        // arrange
        ParkingSession session = ParkingSessionFixture.aSession().enteredAt(ENTRY).build();
        session.exit(EXIT, Money.ZERO, PricingStrategyType.STANDARD);

        // act / assert
        assertThrows(ParkingSessionAlreadyExitedException.class, () -> session.exit(EXIT, Money.ZERO, PricingStrategyType.STANDARD),
                "must not exit a session twice");
    }
}
