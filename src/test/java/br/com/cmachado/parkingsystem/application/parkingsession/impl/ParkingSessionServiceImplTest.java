package br.com.cmachado.parkingsystem.application.parkingsession.impl;

import br.com.cmachado.parkingsystem.domain.service.pricing.ChargeCalculator;
import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.LicensePlate;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionStatus;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.events.VehicleExited;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.events.VehicleParked;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.GeoLocation;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.events.SpotOccupied;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.events.SpotReleased;
import br.com.cmachado.parkingsystem.infrastructure.http.BadRequestException;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.violations.GarageFullException;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.violations.ParkingSessionNotFoundException;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.violations.ParkingSpotNotFoundException;
import br.com.cmachado.parkingsystem.fixtures.ParkingSessionFixture;
import br.com.cmachado.parkingsystem.fixtures.ParkingSpotFixture;
import br.com.cmachado.parkingsystem.fixtures.WebhookEventFixture;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookEventRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static br.com.cmachado.parkingsystem.support.DomainEventAssertions.assertHasEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingSessionServiceImplTest {

    private static final LocalDateTime ENTRY = LocalDateTime.parse("2025-01-01T10:00:00");

    @Mock private ParkingSessionRepository sessionRepository;
    @Mock private ParkingSpotRepository spotRepository;
    @Mock private ChargeCalculator chargeCalculator;

    private ParkingSessionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ParkingSessionServiceImpl(
                sessionRepository, spotRepository, chargeCalculator, new SimpleMeterRegistry());
    }

    // ── validation ───────────────────────────────────────────────────────────

    @Test
    void missingLicensePlateThrows400() {
        // arrange
        WebhookEventRequest request = WebhookEventFixture.anEntry().withPlate(null).at(ENTRY).build();

        // act / assert
        assertThrows(BadRequestException.class, () -> service.handle(request),
                "missing license plate must be rejected");
    }

    @Test
    void missingEventTypeThrows400() {
        // arrange
        WebhookEventRequest request = WebhookEventFixture.ofType(null).withPlate("ZUL0001").build();

        // act / assert
        assertThrows(BadRequestException.class, () -> service.handle(request),
                "missing event type must be rejected");
    }

    @Test
    void unknownEventTypeThrows400() {
        // arrange
        WebhookEventRequest request = WebhookEventFixture.ofType("TELEPORT").withPlate("ZUL0001").build();

        // act / assert
        assertThrows(BadRequestException.class, () -> service.handle(request),
                "unknown event type must be rejected");
    }

    @Test
    void entryWithoutEntryTimeThrows400() {
        // arrange
        WebhookEventRequest request = WebhookEventFixture.anEntry().withPlate("ZUL0001").build();

        // act / assert
        assertThrows(BadRequestException.class, () -> service.handle(request),
                "ENTRY without entry_time must be rejected");
    }

    @Test
    void parkedWithoutLatLngThrows400() {
        // arrange
        WebhookEventRequest request = WebhookEventFixture.aParked().withPlate("ZUL0001").build();

        // act / assert
        assertThrows(BadRequestException.class, () -> service.handle(request),
                "PARKED without lat/lng must be rejected");
    }

    @Test
    void exitWithoutExitTimeThrows400() {
        // arrange
        WebhookEventRequest request = WebhookEventFixture.anExit().withPlate("ZUL0001").build();

        // act / assert
        assertThrows(BadRequestException.class, () -> service.handle(request),
                "EXIT without exit_time must be rejected");
    }

    // ── ENTRY ─────────────────────────────────────────────────────────────────

    @Test
    void entryWhenGarageFullThrowsGarageFullException() {
        // arrange
        when(spotRepository.existsAvailableSpotInOpenSector(any())).thenReturn(false);

        // act / assert
        assertThrows(GarageFullException.class,
                () -> service.handle(entry("FULL123")),
                "no open-sector spot available must reject entry");
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void entryWhenGarageHasCapacityStoresSession() {
        // arrange
        when(spotRepository.existsAvailableSpotInOpenSector(any())).thenReturn(true);

        // act
        service.handle(entry("OPEN123"));

        // assert
        verify(sessionRepository).save(any(ParkingSession.class));
    }

    // ── PARKED ────────────────────────────────────────────────────────────────

    @Test
    void parkedThrowsWhenSessionNotFound() {
        // arrange
        when(sessionRepository.findByLicensePlateAndStatusIn(any(), any())).thenReturn(Optional.empty());

        // act / assert
        assertThrows(ParkingSessionNotFoundException.class,
                () -> service.handle(parked("NOPE01", 10.0, 10.0)),
                "no ENTERED session must reject PARKED");
    }

    @Test
    void parkedThrowsWhenSpotNotFound() {
        // arrange
        ParkingSession session = enteredSession("CAR0002");
        when(sessionRepository.findByLicensePlateAndStatusIn(LicensePlate.of("CAR0002"), List.of(ParkingSessionStatus.ENTERED)))
                .thenReturn(Optional.of(session));
        when(spotRepository.findByLocation(any())).thenReturn(Optional.empty());

        // act / assert
        assertThrows(ParkingSpotNotFoundException.class,
                () -> service.handle(parked("CAR0002", 10.0, 10.0)),
                "no spot at location must reject PARKED");
    }

    @Test
    void parkedOccupiesSpotParksSessionAndSavesBoth() {
        // arrange
        ParkingSession session = enteredSession("CAR0001");
        ParkingSpot parkSpot = ParkingSpotFixture.aSpot().withExternalId(1L).withSector("A").withLocation(10.0, 10.0).build();
        when(sessionRepository.findByLicensePlateAndStatusIn(LicensePlate.of("CAR0001"), List.of(ParkingSessionStatus.ENTERED)))
                .thenReturn(Optional.of(session));
        when(spotRepository.findByLocation(GeoLocation.of(10.0, 10.0))).thenReturn(Optional.of(parkSpot));

        // act
        service.handle(parked("CAR0001", 10.0, 10.0));

        // assert
        assertEquals(ParkingSessionStatus.PARKED, session.getStatus(), "session must be PARKED");
        assertTrue(parkSpot.isOccupied(), "spot must be occupied");
        assertHasEvent(session, VehicleParked.class);
        assertHasEvent(parkSpot, SpotOccupied.class);
        verify(spotRepository).save(parkSpot);
        verify(sessionRepository).save(session);
    }

    // ── EXIT ──────────────────────────────────────────────────────────────────

    @Test
    void exitFromEnteredSessionCallsChargeAndSavesSession() {
        // arrange
        ParkingSession session = enteredSession("EXIT001");
        when(sessionRepository.findByLicensePlateAndStatusIn(
                LicensePlate.of("EXIT001"), List.of(ParkingSessionStatus.ENTERED, ParkingSessionStatus.PARKED)))
                .thenReturn(Optional.of(session));
        chargeExits(Money.ZERO);

        // act
        service.handle(exit("EXIT001", LocalDateTime.parse("2025-01-01T11:00:00")));

        // assert
        assertEquals(ParkingSessionStatus.EXITED, session.getStatus(), "session must be EXITED");
        assertHasEvent(session, VehicleExited.class);
        verify(chargeCalculator).charge(eq(session), any(LocalDateTime.class));
        verify(sessionRepository).save(session);
        verify(spotRepository, never()).save(any(ParkingSpot.class));
    }

    @Test
    void exitThrowsWhenSpotNotFound() {
        // arrange
        ParkingSpot theSpot = ParkingSpotFixture.aSpot().withExternalId(1L).build();
        ParkingSession session = ParkingSessionFixture.aSession().withPlate("EXIT002").enteredAt(ENTRY).parkedOn(theSpot).build();
        when(sessionRepository.findByLicensePlateAndStatusIn(
                LicensePlate.of("EXIT002"), List.of(ParkingSessionStatus.ENTERED, ParkingSessionStatus.PARKED)))
                .thenReturn(Optional.of(session));
        when(spotRepository.findById(session.getSpotId())).thenReturn(Optional.empty());

        // act / assert
        assertThrows(ParkingSpotNotFoundException.class,
                () -> service.handle(exit("EXIT002", LocalDateTime.parse("2025-01-01T12:00:00"))),
                "missing spot for a PARKED session must throw");
    }

    @Test
    void exitReleasesSpotAndSavesSession() {
        // arrange
        ParkingSpot theSpot = ParkingSpotFixture.aSpot().withExternalId(1L).build();
        ParkingSession session = ParkingSessionFixture.aSession().withPlate("EXIT003").enteredAt(ENTRY).parkedOn(theSpot).build();
        when(sessionRepository.findByLicensePlateAndStatusIn(
                LicensePlate.of("EXIT003"), List.of(ParkingSessionStatus.ENTERED, ParkingSessionStatus.PARKED)))
                .thenReturn(Optional.of(session));
        when(spotRepository.findById(theSpot.getId())).thenReturn(Optional.of(theSpot));
        chargeExits(Money.of("15.00"));

        // act
        service.handle(exit("EXIT003", LocalDateTime.parse("2025-01-01T12:00:00")));

        // assert
        assertEquals(ParkingSessionStatus.EXITED, session.getStatus(), "session must be EXITED");
        assertFalse(theSpot.isOccupied(), "spot must be released");
        assertHasEvent(session, VehicleExited.class);
        assertHasEvent(theSpot, SpotReleased.class);
        verify(chargeCalculator).charge(eq(session), any(LocalDateTime.class));
        verify(spotRepository).save(theSpot);
        verify(sessionRepository).save(session);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Stubs the charge calculator to apply {@code amount} and exit the session it receives. */
    private void chargeExits(Money amount) {
        doAnswer(inv -> {
            ((ParkingSession) inv.getArgument(0)).exit(inv.getArgument(1), amount);
            return null;
        }).when(chargeCalculator).charge(any(ParkingSession.class), any(LocalDateTime.class));
    }

    private ParkingSession enteredSession(String plate) {
        return ParkingSessionFixture.aSession().withPlate(plate).enteredAt(ENTRY).build();
    }

    private WebhookEventRequest entry(String plate) {
        return WebhookEventFixture.anEntry().withPlate(plate).at(ENTRY).build();
    }

    private WebhookEventRequest parked(String plate, Double lat, Double lng) {
        return WebhookEventFixture.aParked().withPlate(plate).atLocation(lat, lng).build();
    }

    private WebhookEventRequest exit(String plate, LocalDateTime exitTime) {
        return WebhookEventFixture.anExit().withPlate(plate).at(exitTime).build();
    }
}
