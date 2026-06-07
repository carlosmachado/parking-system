package br.com.cmachado.parkingsystem.application.parkingsession.webhook;

import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.type.EntryWebhookEventHandler;
import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.WebhookEventType;
import br.com.cmachado.parkingsystem.application.parkingsession.webhook.handler.type.WebhookEventValidationException;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.LicensePlate;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionStatus;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.PricingElection;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.violations.GarageFullException;
import br.com.cmachado.parkingsystem.domain.service.pricing.ChargeCalculator;
import br.com.cmachado.parkingsystem.fixtures.WebhookEventFixture;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookEventRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntryWebhookEventHandlerTest {

    private static final LocalDateTime ENTRY = LocalDateTime.parse("2025-01-01T10:00:00");

    @Mock private ParkingSessionRepository sessionRepository;
    @Mock private ParkingSpotRepository spotRepository;
    @Mock private ChargeCalculator chargeCalculator;

    private EntryWebhookEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new EntryWebhookEventHandler(sessionRepository, spotRepository, chargeCalculator, new SimpleMeterRegistry());
    }

    @Test
    void eventTypeIsEntry() {
        org.junit.jupiter.api.Assertions.assertEquals(WebhookEventType.ENTRY, handler.eventType());
    }

    @Test
    void missingEntryTimeThrows400() {
        WebhookEventRequest request = WebhookEventFixture.anEntry().withPlate("ZUL0001").build();

        assertThrows(WebhookEventValidationException.class, () -> handler.handle(request),
                "ENTRY without entry_time must be rejected");
    }

    @Test
    void invalidEntryTimeThrows400() {
        WebhookEventRequest request = WebhookEventFixture.anEntry()
                .withPlate("ZUL0001")
                .atRaw("not-a-date")
                .build();

        assertThrows(WebhookEventValidationException.class, () -> handler.handle(request),
                "ENTRY with invalid entry_time must be rejected");
    }

    @Test
    void garageFullThrowsGarageFullException() {
        when(spotRepository.existsAvailableSpotInOpenSector(any())).thenReturn(false);

        assertThrows(GarageFullException.class,
                () -> handler.handle(WebhookEventFixture.anEntry().withPlate("FULL123").at(ENTRY).build()),
                "no open-sector spot available must reject entry");
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void garageWithCapacityStoresSession() {
        when(spotRepository.existsAvailableSpotInOpenSector(any())).thenReturn(true);
        when(chargeCalculator.electOnEntry())
                .thenReturn(new ChargeCalculator.EntryPricing(PricingElection.AT_EXIT, null));

        handler.handle(WebhookEventFixture.anEntry().withPlate("OPEN123").at(ENTRY).build());

        verify(sessionRepository).save(any(ParkingSession.class));
    }

    @Test
    void duplicateEntryForActiveSessionIsIgnored() {
        ParkingSession activeSession = ParkingSession.start(LicensePlate.of("OPEN123"), ENTRY)
                .build();
        when(sessionRepository.findByLicensePlateAndStatusIn(
                LicensePlate.of("OPEN123"),
                List.of(ParkingSessionStatus.ENTERED, ParkingSessionStatus.PARKED)))
                .thenReturn(Optional.of(activeSession));

        handler.handle(WebhookEventFixture.anEntry().withPlate("OPEN123").at(ENTRY).build());

        verify(spotRepository, never()).existsAvailableSpotInOpenSector(any());
        verify(chargeCalculator, never()).electOnEntry();
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void availabilityUsesEntryEventTime() {
        when(spotRepository.existsAvailableSpotInOpenSector(any())).thenReturn(true);
        when(chargeCalculator.electOnEntry())
                .thenReturn(new ChargeCalculator.EntryPricing(PricingElection.AT_EXIT, null));

        handler.handle(WebhookEventFixture.anEntry().withPlate("TIME123")
                .at(LocalDateTime.parse("2025-01-01T22:30:00"))
                .build());

        ArgumentCaptor<LocalTime> timeCaptor = ArgumentCaptor.forClass(LocalTime.class);
        verify(spotRepository).existsAvailableSpotInOpenSector(timeCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(22, 30), timeCaptor.getValue());
    }
}
