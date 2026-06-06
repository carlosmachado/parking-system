package br.com.cmachado.parkingsystem.application.webhook.impl;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.LicensePlate;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionStatus;
import br.com.cmachado.parkingsystem.domain.model.sector.Sector;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorRepository;
import br.com.cmachado.parkingsystem.domain.model.spot.GeoLocation;
import br.com.cmachado.parkingsystem.domain.model.spot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.spot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.domain.service.occupancy.OccupancyDomainService;
import br.com.cmachado.parkingsystem.domain.service.pricing.DiscountPricingStrategy;
import br.com.cmachado.parkingsystem.domain.service.pricing.PricingStrategyFactory;
import br.com.cmachado.parkingsystem.domain.service.pricing.StandardPricingStrategy;
import br.com.cmachado.parkingsystem.domain.service.pricing.Surcharge10PricingStrategy;
import br.com.cmachado.parkingsystem.domain.service.pricing.Surcharge25PricingStrategy;
import br.com.cmachado.parkingsystem.infrastructure.http.GarageFullException;
import br.com.cmachado.parkingsystem.infrastructure.http.ParkingSessionNotFoundException;
import br.com.cmachado.parkingsystem.infrastructure.http.ParkingSpotNotFoundException;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookEventRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingSessionServiceImplTest {

    @Mock
    private ParkingSessionRepository sessionRepository;

    @Mock
    private ParkingSpotRepository spotRepository;

    @Mock
    private SectorRepository sectorRepository;

    private ParkingSessionServiceImpl service;

    @BeforeEach
    void setUp() {
        PricingStrategyFactory pricingStrategyFactory = new PricingStrategyFactory(
                new DiscountPricingStrategy(),
                new StandardPricingStrategy(),
                new Surcharge10PricingStrategy(),
                new Surcharge25PricingStrategy());
        service = new ParkingSessionServiceImpl(
                sessionRepository,
                spotRepository,
                sectorRepository,
                new OccupancyDomainService(),
                pricingStrategyFactory,
                new SimpleMeterRegistry());
    }

    @Test
    void entryWhenGarageFullThrowsGarageFullException() {
        when(spotRepository.existsByOccupiedFalse()).thenReturn(false);

        assertThrows(GarageFullException.class,
                () -> service.processEntry(entry("FULL123", LocalDateTime.parse("2025-01-01T10:00:00"))));

        verify(sessionRepository, never()).save(any());
    }

    @Test
    void entryWhenGarageHasCapacityStoresSession() {
        when(spotRepository.existsByOccupiedFalse()).thenReturn(true);

        service.processEntry(entry("OPEN123", LocalDateTime.parse("2025-01-01T10:00:00")));

        verify(sessionRepository).save(any(ParkingSession.class));
    }

    @Test
    void parkedThrowsWhenSessionNotFound() {
        when(sessionRepository.findByLicensePlateAndStatusIn(any(), any())).thenReturn(Optional.empty());

        assertThrows(ParkingSessionNotFoundException.class,
                () -> service.processParked(parked("NOPE01", 10.0, 10.0)));
    }

    @Test
    void parkedThrowsWhenSpotNotFound() {
        ParkingSession session = entered("CAR0002");
        when(sessionRepository.findByLicensePlateAndStatusIn(LicensePlate.of("CAR0002"), List.of(ParkingSessionStatus.ENTERED)))
                .thenReturn(Optional.of(session));
        when(spotRepository.findByLocation(any())).thenReturn(Optional.empty());

        assertThrows(ParkingSpotNotFoundException.class,
                () -> service.processParked(parked("CAR0002", 10.0, 10.0)));
    }

    @Test
    void parkedSavesSessionAndSpot() {
        ParkingSession session = entered("CAR0001");
        ParkingSpot parkSpot = spot(1L, "A", 10.0, 10.0);
        when(sessionRepository.findByLicensePlateAndStatusIn(LicensePlate.of("CAR0001"), List.of(ParkingSessionStatus.ENTERED)))
                .thenReturn(Optional.of(session));
        when(spotRepository.findByLocation(GeoLocation.of(10.0, 10.0))).thenReturn(Optional.of(parkSpot));

        service.processParked(parked("CAR0001", 10.0, 10.0));

        assertEquals(ParkingSessionStatus.PARKED, session.getStatus());
        assertTrue(parkSpot.isOccupied());
        verify(spotRepository).save(parkSpot);
        verify(sessionRepository).save(session);
    }

    @Test
    void exitFromEnteredSessionChargesZero() {
        ParkingSession session = entered("EXIT001");
        when(sessionRepository.findByLicensePlateAndStatusIn(
                LicensePlate.of("EXIT001"), List.of(ParkingSessionStatus.ENTERED, ParkingSessionStatus.PARKED)))
                .thenReturn(Optional.of(session));

        service.processExit(exit("EXIT001", LocalDateTime.parse("2025-01-01T11:00:00")));

        assertEquals(ParkingSessionStatus.EXITED, session.getStatus());
        assertEquals(BigDecimal.ZERO.setScale(2), session.getAmountCharged().getAmount());
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
                () -> service.processExit(exit("EXIT002", LocalDateTime.parse("2025-01-01T12:00:00"))));
    }

    @Test
    void exitWithMissingSectorChargesZeroAndReleasesSpot() {
        ParkingSpot theSpot = spot(1L, "A", 10.0, 10.0);
        ParkingSession session = parkedSession("EXIT003", theSpot);
        when(sessionRepository.findByLicensePlateAndStatusIn(
                LicensePlate.of("EXIT003"), List.of(ParkingSessionStatus.ENTERED, ParkingSessionStatus.PARKED)))
                .thenReturn(Optional.of(session));
        when(spotRepository.findById(theSpot.getId())).thenReturn(Optional.of(theSpot));
        when(sectorRepository.findByCode(SectorCode.of("A"))).thenReturn(Optional.empty());
        when(sessionRepository.existsBySpotIdAndStatusAndIdNot(theSpot.getId(), ParkingSessionStatus.PARKED, session.getId()))
                .thenReturn(false);

        service.processExit(exit("EXIT003", LocalDateTime.parse("2025-01-01T12:00:00")));

        assertEquals(ParkingSessionStatus.EXITED, session.getStatus());
        assertEquals(BigDecimal.ZERO.setScale(2), session.getAmountCharged().getAmount());
        assertFalse(theSpot.isOccupied());
        verify(spotRepository).save(theSpot);
        verify(sessionRepository).save(session);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private WebhookEventRequest entry(String plate, LocalDateTime entryTime) {
        WebhookEventRequest request = new WebhookEventRequest();
        request.setLicensePlate(plate);
        request.setEntryTime(entryTime.toString());
        request.setEventType("ENTRY");
        return request;
    }

    private WebhookEventRequest parked(String plate, Double lat, Double lng) {
        WebhookEventRequest request = new WebhookEventRequest();
        request.setLicensePlate(plate);
        request.setLat(lat);
        request.setLng(lng);
        request.setEventType("PARKED");
        return request;
    }

    private WebhookEventRequest exit(String plate, LocalDateTime exitTime) {
        WebhookEventRequest request = new WebhookEventRequest();
        request.setLicensePlate(plate);
        request.setExitTime(exitTime.toString());
        request.setEventType("EXIT");
        return request;
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

    private Sector sector(String code, LocalTime openHour, LocalTime closeHour) {
        return Sector.register(SectorCode.of(code), Money.of("10.00"), 10, openHour, closeHour, 1440);
    }
}
