package br.com.cmachado.parkingsystem.application.parkingsession.impl;

import br.com.cmachado.parkingsystem.domain.service.pricing.ChargeCalculator;
import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.LicensePlate;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionStatus;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.GeoLocation;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.infrastructure.http.BadRequestException;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.GarageFullException;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionNotFoundException;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpotNotFoundException;
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
        WebhookEventRequest req = new WebhookEventRequest();
        req.setEventType("ENTRY");
        assertThrows(BadRequestException.class, () -> service.handle(req));
    }

    @Test
    void missingEventTypeThrows400() {
        WebhookEventRequest req = new WebhookEventRequest();
        req.setLicensePlate("ZUL0001");
        assertThrows(BadRequestException.class, () -> service.handle(req));
    }

    @Test
    void unknownEventTypeThrows400() {
        assertThrows(BadRequestException.class,
                () -> service.handle(request("ZUL0001", "TELEPORT")));
    }

    @Test
    void entryWithoutEntryTimeThrows400() {
        WebhookEventRequest req = new WebhookEventRequest();
        req.setLicensePlate("ZUL0001");
        req.setEventType("ENTRY");
        assertThrows(BadRequestException.class, () -> service.handle(req));
    }

    @Test
    void parkedWithoutLatLngThrows400() {
        WebhookEventRequest req = new WebhookEventRequest();
        req.setLicensePlate("ZUL0001");
        req.setEventType("PARKED");
        assertThrows(BadRequestException.class, () -> service.handle(req));
    }

    @Test
    void exitWithoutExitTimeThrows400() {
        WebhookEventRequest req = new WebhookEventRequest();
        req.setLicensePlate("ZUL0001");
        req.setEventType("EXIT");
        assertThrows(BadRequestException.class, () -> service.handle(req));
    }

    // ── ENTRY ─────────────────────────────────────────────────────────────────

    @Test
    void entryWhenGarageFullThrowsGarageFullException() {
        when(spotRepository.existsAvailableSpotInOpenSector(any())).thenReturn(false);

        assertThrows(GarageFullException.class,
                () -> service.handle(entry("FULL123", LocalDateTime.parse("2025-01-01T10:00:00"))));

        verify(sessionRepository, never()).save(any());
    }

    @Test
    void entryRejectedWhenAllSectorsClosed() {
        when(spotRepository.existsAvailableSpotInOpenSector(any())).thenReturn(false);

        assertThrows(GarageFullException.class,
                () -> service.handle(entry("CLOSED1", LocalDateTime.parse("2025-01-01T10:00:00"))));

        verify(sessionRepository, never()).save(any());
    }

    @Test
    void entryWhenGarageHasCapacityStoresSession() {
        when(spotRepository.existsAvailableSpotInOpenSector(any())).thenReturn(true);

        service.handle(entry("OPEN123", LocalDateTime.parse("2025-01-01T10:00:00")));

        verify(sessionRepository).save(any(ParkingSession.class));
    }

    // ── PARKED ────────────────────────────────────────────────────────────────

    @Test
    void parkedThrowsWhenSessionNotFound() {
        when(sessionRepository.findByLicensePlateAndStatusIn(any(), any())).thenReturn(Optional.empty());

        assertThrows(ParkingSessionNotFoundException.class,
                () -> service.handle(parked("NOPE01", 10.0, 10.0)));
    }

    @Test
    void parkedThrowsWhenSpotNotFound() {
        ParkingSession session = entered("CAR0002");
        when(sessionRepository.findByLicensePlateAndStatusIn(LicensePlate.of("CAR0002"), List.of(ParkingSessionStatus.ENTERED)))
                .thenReturn(Optional.of(session));
        when(spotRepository.findByLocation(any())).thenReturn(Optional.empty());

        assertThrows(ParkingSpotNotFoundException.class,
                () -> service.handle(parked("CAR0002", 10.0, 10.0)));
    }

    @Test
    void parkedSavesSessionAndSpot() {
        ParkingSession session = entered("CAR0001");
        ParkingSpot parkSpot = spot(1L, "A", 10.0, 10.0);
        when(sessionRepository.findByLicensePlateAndStatusIn(LicensePlate.of("CAR0001"), List.of(ParkingSessionStatus.ENTERED)))
                .thenReturn(Optional.of(session));
        when(spotRepository.findByLocation(GeoLocation.of(10.0, 10.0))).thenReturn(Optional.of(parkSpot));

        service.handle(parked("CAR0001", 10.0, 10.0));

        assertEquals(ParkingSessionStatus.PARKED, session.getStatus());
        assertTrue(parkSpot.isOccupied());
        verify(spotRepository).save(parkSpot);
        verify(sessionRepository).save(session);
    }

    // ── EXIT ──────────────────────────────────────────────────────────────────

    @Test
    void exitFromEnteredSessionCallsChargeAndSavesSession() {
        ParkingSession session = entered("EXIT001");
        when(sessionRepository.findByLicensePlateAndStatusIn(
                LicensePlate.of("EXIT001"), List.of(ParkingSessionStatus.ENTERED, ParkingSessionStatus.PARKED)))
                .thenReturn(Optional.of(session));
        doAnswer(inv -> {
            ((ParkingSession) inv.getArgument(0)).exit(inv.getArgument(1), Money.ZERO);
            return null;
        }).when(chargeCalculator).charge(any(ParkingSession.class), any(LocalDateTime.class));

        service.handle(exit("EXIT001", LocalDateTime.parse("2025-01-01T11:00:00")));

        assertEquals(ParkingSessionStatus.EXITED, session.getStatus());
        verify(chargeCalculator).charge(eq(session), any(LocalDateTime.class));
        verify(sessionRepository).save(session);
        verify(spotRepository, never()).save(any(ParkingSpot.class));
    }

    @Test
    void exitThrowsWhenSpotNotFound() {
        ParkingSpot theSpot = spot(1L, "A", 10.0, 10.0);
        ParkingSession session = parkedSession("EXIT002", theSpot);
        when(sessionRepository.findByLicensePlateAndStatusIn(
                LicensePlate.of("EXIT002"), List.of(ParkingSessionStatus.ENTERED, ParkingSessionStatus.PARKED)))
                .thenReturn(Optional.of(session));
        when(spotRepository.findById(session.getSpotId())).thenReturn(Optional.empty());

        assertThrows(ParkingSpotNotFoundException.class,
                () -> service.handle(exit("EXIT002", LocalDateTime.parse("2025-01-01T12:00:00"))));
    }

    @Test
    void exitReleasesSpotAndSavesSession() {
        ParkingSpot theSpot = spot(1L, "A", 10.0, 10.0);
        ParkingSession session = parkedSession("EXIT003", theSpot);
        when(sessionRepository.findByLicensePlateAndStatusIn(
                LicensePlate.of("EXIT003"), List.of(ParkingSessionStatus.ENTERED, ParkingSessionStatus.PARKED)))
                .thenReturn(Optional.of(session));
        when(spotRepository.findById(theSpot.getId())).thenReturn(Optional.of(theSpot));
        doAnswer(inv -> {
            ((ParkingSession) inv.getArgument(0)).exit(inv.getArgument(1), Money.of("15.00"));
            return null;
        }).when(chargeCalculator).charge(any(ParkingSession.class), any(LocalDateTime.class));

        service.handle(exit("EXIT003", LocalDateTime.parse("2025-01-01T12:00:00")));

        assertEquals(ParkingSessionStatus.EXITED, session.getStatus());
        assertFalse(theSpot.isOccupied());
        verify(chargeCalculator).charge(eq(session), any(LocalDateTime.class));
        verify(spotRepository).save(theSpot);
        verify(sessionRepository).save(session);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private WebhookEventRequest request(String plate, String eventType) {
        WebhookEventRequest req = new WebhookEventRequest();
        req.setLicensePlate(plate);
        req.setEventType(eventType);
        return req;
    }

    private WebhookEventRequest entry(String plate, LocalDateTime entryTime) {
        WebhookEventRequest req = new WebhookEventRequest();
        req.setLicensePlate(plate);
        req.setEntryTime(entryTime.toString());
        req.setEventType("ENTRY");
        return req;
    }

    private WebhookEventRequest parked(String plate, Double lat, Double lng) {
        WebhookEventRequest req = new WebhookEventRequest();
        req.setLicensePlate(plate);
        req.setLat(lat);
        req.setLng(lng);
        req.setEventType("PARKED");
        return req;
    }

    private WebhookEventRequest exit(String plate, LocalDateTime exitTime) {
        WebhookEventRequest req = new WebhookEventRequest();
        req.setLicensePlate(plate);
        req.setExitTime(exitTime.toString());
        req.setEventType("EXIT");
        return req;
    }

    private ParkingSession entered(String plate) {
        return ParkingSession.enter(LicensePlate.of(plate), LocalDateTime.parse("2025-01-01T10:00:00"));
    }

    private ParkingSession parkedSession(String plate, ParkingSpot parkingSpot) {
        ParkingSession session = entered(plate);
        parkingSpot.park(session);
        return session;
    }

    private ParkingSpot spot(Long externalId, String sectorCode, double lat, double lng) {
        return ParkingSpot.register(externalId, SectorCode.of(sectorCode), GeoLocation.of(lat, lng));
    }

}
