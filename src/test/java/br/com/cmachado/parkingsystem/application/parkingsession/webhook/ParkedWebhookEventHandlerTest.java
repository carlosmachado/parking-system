package br.com.cmachado.parkingsystem.application.parkingsession.webhook;

import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.type.ParkedWebhookEventHandler;
import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.WebhookEventType;
import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.type.WebhookEventValidationException;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.LicensePlate;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionStatus;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.events.VehicleParked;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.violations.ParkingSessionNotFoundException;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.GeoLocation;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.events.ParkingSpotOccupied;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.violations.ParkingSpotNotFoundException;
import br.com.cmachado.parkingsystem.fixtures.ParkingSessionFixture;
import br.com.cmachado.parkingsystem.fixtures.ParkingSpotFixture;
import br.com.cmachado.parkingsystem.fixtures.WebhookEventFixture;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookEventRequest;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkedWebhookEventHandlerTest {

    private static final LocalDateTime ENTRY = LocalDateTime.parse("2025-01-01T10:00:00");

    @Mock private ParkingSessionRepository sessionRepository;
    @Mock private ParkingSpotRepository spotRepository;

    private ParkedWebhookEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ParkedWebhookEventHandler(sessionRepository, spotRepository);
    }

    @Test
    void eventTypeIsParked() {
        assertEquals(WebhookEventType.PARKED, handler.eventType());
    }

    @Test
    void missingLatLngThrows400() {
        WebhookEventRequest request = WebhookEventFixture.aParked().withPlate("ZUL0001").build();

        assertThrows(WebhookEventValidationException.class, () -> handler.handle(request),
                "PARKED without lat/lng must be rejected");
    }

    @Test
    void sessionNotFoundThrows() {
        when(sessionRepository.findByLicensePlateAndStatusIn(any(), any())).thenReturn(Optional.empty());

        assertThrows(ParkingSessionNotFoundException.class,
                () -> handler.handle(WebhookEventFixture.aParked().withPlate("NOPE01").atLocation(10.0, 10.0).build()),
                "no ENTERED session must reject PARKED");
    }

    @Test
    void spotNotFoundThrows() {
        ParkingSession session = enteredSession("CAR0002");
        when(sessionRepository.findByLicensePlateAndStatusIn(LicensePlate.of("CAR0002"), List.of(ParkingSessionStatus.ENTERED)))
                .thenReturn(Optional.of(session));
        when(spotRepository.findByLocation(any())).thenReturn(Optional.empty());

        assertThrows(ParkingSpotNotFoundException.class,
                () -> handler.handle(WebhookEventFixture.aParked().withPlate("CAR0002").atLocation(10.0, 10.0).build()),
                "no spot at location must reject PARKED");
    }

    @Test
    void occupiesSpotParksSessionAndSavesBoth() {
        ParkingSession session = enteredSession("CAR0001");
        ParkingSpot parkSpot = ParkingSpotFixture.aSpot().withExternalId(1L).withSector("A").withLocation(10.0, 10.0).build();
        when(sessionRepository.findByLicensePlateAndStatusIn(LicensePlate.of("CAR0001"), List.of(ParkingSessionStatus.ENTERED)))
                .thenReturn(Optional.of(session));
        when(spotRepository.findByLocation(GeoLocation.of(10.0, 10.0))).thenReturn(Optional.of(parkSpot));

        handler.handle(WebhookEventFixture.aParked().withPlate("CAR0001").atLocation(10.0, 10.0).build());

        assertEquals(ParkingSessionStatus.PARKED, session.getStatus(), "session must be PARKED");
        assertTrue(parkSpot.isOccupied(), "spot must be occupied");
        assertHasEvent(session, VehicleParked.class);
        assertHasEvent(parkSpot, ParkingSpotOccupied.class);
        verify(spotRepository).save(parkSpot);
        verify(sessionRepository).save(session);
    }

    private ParkingSession enteredSession(String plate) {
        return ParkingSessionFixture.aSession().withPlate(plate).enteredAt(ENTRY).build();
    }
}
