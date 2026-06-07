package br.com.cmachado.parkingsystem.application.parkingsession.webhook;

import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.type.ExitWebhookEventHandler;
import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.WebhookEventType;
import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.type.WebhookEventValidationException;
import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.LicensePlate;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionStatus;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.events.VehicleExited;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.events.SpotReleased;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.violations.ParkingSpotNotFoundException;
import br.com.cmachado.parkingsystem.domain.service.pricing.ChargeCalculator;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExitWebhookEventHandlerTest {

    private static final LocalDateTime ENTRY = LocalDateTime.parse("2025-01-01T10:00:00");

    @Mock private ParkingSessionRepository sessionRepository;
    @Mock private ParkingSpotRepository spotRepository;
    @Mock private ChargeCalculator chargeCalculator;

    private ExitWebhookEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ExitWebhookEventHandler(sessionRepository, spotRepository, chargeCalculator);
    }

    @Test
    void eventTypeIsExit() {
        assertEquals(WebhookEventType.EXIT, handler.eventType());
    }

    @Test
    void missingExitTimeThrows400() {
        WebhookEventRequest request = WebhookEventFixture.anExit().withPlate("ZUL0001").build();

        assertThrows(WebhookEventValidationException.class, () -> handler.handle(request),
                "EXIT without exit_time must be rejected");
    }

    @Test
    void enteredSessionCallsChargeAndSavesSession() {
        ParkingSession session = enteredSession("EXIT001");
        when(sessionRepository.findByLicensePlateAndStatusIn(
                LicensePlate.of("EXIT001"), List.of(ParkingSessionStatus.ENTERED, ParkingSessionStatus.PARKED)))
                .thenReturn(Optional.of(session));
        chargeExits(Money.ZERO);

        handler.handle(WebhookEventFixture.anExit().withPlate("EXIT001").at(LocalDateTime.parse("2025-01-01T11:00:00")).build());

        assertEquals(ParkingSessionStatus.EXITED, session.getStatus(), "session must be EXITED");
        assertHasEvent(session, VehicleExited.class);
        verify(chargeCalculator).charge(eq(session), any(LocalDateTime.class));
        verify(sessionRepository).save(session);
        verify(spotRepository, never()).save(any(ParkingSpot.class));
    }

    @Test
    void missingSpotThrowsForParkedSession() {
        ParkingSpot theSpot = ParkingSpotFixture.aSpot().withExternalId(1L).build();
        ParkingSession session = ParkingSessionFixture.aSession().withPlate("EXIT002").enteredAt(ENTRY).parkedOn(theSpot).build();
        when(sessionRepository.findByLicensePlateAndStatusIn(
                LicensePlate.of("EXIT002"), List.of(ParkingSessionStatus.ENTERED, ParkingSessionStatus.PARKED)))
                .thenReturn(Optional.of(session));
        when(spotRepository.findById(session.getSpotId())).thenReturn(Optional.empty());

        assertThrows(ParkingSpotNotFoundException.class,
                () -> handler.handle(WebhookEventFixture.anExit().withPlate("EXIT002").at(LocalDateTime.parse("2025-01-01T12:00:00")).build()),
                "missing spot for a PARKED session must throw");
    }

    @Test
    void releasesSpotAndSavesSession() {
        ParkingSpot theSpot = ParkingSpotFixture.aSpot().withExternalId(1L).build();
        ParkingSession session = ParkingSessionFixture.aSession().withPlate("EXIT003").enteredAt(ENTRY).parkedOn(theSpot).build();
        when(sessionRepository.findByLicensePlateAndStatusIn(
                LicensePlate.of("EXIT003"), List.of(ParkingSessionStatus.ENTERED, ParkingSessionStatus.PARKED)))
                .thenReturn(Optional.of(session));
        when(spotRepository.findById(theSpot.getId())).thenReturn(Optional.of(theSpot));
        chargeExits(Money.of("15.00"));

        handler.handle(WebhookEventFixture.anExit().withPlate("EXIT003").at(LocalDateTime.parse("2025-01-01T12:00:00")).build());

        assertEquals(ParkingSessionStatus.EXITED, session.getStatus(), "session must be EXITED");
        assertFalse(theSpot.isOccupied(), "spot must be released");
        assertHasEvent(session, VehicleExited.class);
        assertHasEvent(theSpot, SpotReleased.class);
        verify(chargeCalculator).charge(eq(session), any(LocalDateTime.class));
        verify(spotRepository).save(theSpot);
        verify(sessionRepository).save(session);
    }

    private void chargeExits(Money amount) {
        doAnswer(inv -> {
            ((ParkingSession) inv.getArgument(0)).exit(inv.getArgument(1), amount);
            return null;
        }).when(chargeCalculator).charge(any(ParkingSession.class), any(LocalDateTime.class));
    }

    private ParkingSession enteredSession(String plate) {
        return ParkingSessionFixture.aSession().withPlate(plate).enteredAt(ENTRY).build();
    }
}
