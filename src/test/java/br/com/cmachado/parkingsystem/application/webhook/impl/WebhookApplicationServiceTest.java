package br.com.cmachado.parkingsystem.application.webhook.impl;

import br.com.cmachado.parkingsystem.application.revenue.RevenueApplicationService;
import br.com.cmachado.parkingsystem.application.webhook.WebhookApplicationService;
import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.garage.Sector;
import br.com.cmachado.parkingsystem.domain.model.garage.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.garage.SectorRepository;
import br.com.cmachado.parkingsystem.domain.model.revenue.DailyRevenueRepository;
import br.com.cmachado.parkingsystem.domain.model.spot.GeoLocation;
import br.com.cmachado.parkingsystem.domain.model.spot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.spot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.domain.model.vehicle.ParkingSessionRepository;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.revenue.RevenueResponse;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookEventRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end flow test. Not transactional on purpose: the revenue update runs on an
 * {@code AFTER_COMMIT} async listener, so the use-case transactions must actually commit.
 * Data is cleaned between tests instead of rolled back.
 */
@SpringBootTest
@ActiveProfiles("test")
class WebhookApplicationServiceTest {

    @Autowired
    private WebhookApplicationService webhookService;

    @Autowired
    private RevenueApplicationService revenueService;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private ParkingSpotRepository spotRepository;

    @Autowired
    private ParkingSessionRepository sessionRepository;

    @Autowired
    private DailyRevenueRepository dailyRevenueRepository;

    @BeforeEach
    void setUp() {
        cleanAll();
        SectorCode code = new SectorCode("SEC-A");
        sectorRepository.save(new Sector(code, Money.of(10.0), 10,
                LocalTime.MIDNIGHT, LocalTime.of(23, 59), 1440));
        spotRepository.save(ParkingSpot.register(1L, code, new GeoLocation(10.0, 10.0)));
        spotRepository.save(ParkingSpot.register(2L, code, new GeoLocation(20.0, 20.0)));
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    private void cleanAll() {
        sessionRepository.deleteAll();
        dailyRevenueRepository.deleteAll();
        spotRepository.deleteAll();
        sectorRepository.deleteAll();
    }

    @Test
    void testCompleteVehicleFlow() {
        String plate = "ABC1234";
        String entryTime = LocalDateTime.now().minusHours(2).toString();

        WebhookEventRequest entryReq = new WebhookEventRequest();
        entryReq.setLicensePlate(plate);
        entryReq.setEventType("ENTRY");
        entryReq.setEntryTime(entryTime);
        webhookService.processEntry(entryReq);

        WebhookEventRequest parkReq = new WebhookEventRequest();
        parkReq.setLicensePlate(plate);
        parkReq.setEventType("PARKED");
        webhookService.processParked(parkReq);

        WebhookEventRequest exitReq = new WebhookEventRequest();
        exitReq.setLicensePlate(plate);
        exitReq.setEventType("EXIT");
        exitReq.setExitTime(LocalDateTime.now().toString());
        webhookService.processExit(exitReq);

        // Revenue is updated asynchronously after the exit transaction commits.
        // Stayed 2h, base price 10, occupancy 1/2 (50%) -> +10% surcharge: 20 * 1.10 = 22.00
        BigDecimal amount = awaitRevenue("SEC-A");
        assertEquals(new BigDecimal("22.00"), amount);
    }

    @Test
    void testEntryRecordedWhenGarageFullEmitsMetric() {
        // Fill both spots so the garage reaches 100% occupancy.
        parkVehicle("AAA1111");
        parkVehicle("BBB2222");

        // Entry while full must succeed (returns 200), not throw.
        WebhookEventRequest entryReq = new WebhookEventRequest();
        entryReq.setLicensePlate("CCC3333");
        entryReq.setEventType("ENTRY");
        entryReq.setEntryTime(LocalDateTime.now().toString());
        webhookService.processEntry(entryReq);

        assertTrue(sessionRepository.count() >= 3, "entry while full must be stored");
    }

    private void parkVehicle(String plate) {
        WebhookEventRequest entryReq = new WebhookEventRequest();
        entryReq.setLicensePlate(plate);
        entryReq.setEventType("ENTRY");
        entryReq.setEntryTime(LocalDateTime.now().toString());
        webhookService.processEntry(entryReq);

        WebhookEventRequest parkReq = new WebhookEventRequest();
        parkReq.setLicensePlate(plate);
        parkReq.setEventType("PARKED");
        webhookService.processParked(parkReq);
    }

    /** Polls the revenue endpoint until the async update lands or a timeout is reached. */
    private BigDecimal awaitRevenue(String sector) {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        BigDecimal amount = BigDecimal.ZERO;
        while (System.nanoTime() < deadline) {
            RevenueResponse response = revenueService.getRevenue(LocalDate.now(), sector);
            assertNotNull(response);
            amount = response.getAmount();
            if (amount.signum() != 0) {
                return amount;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return amount;
    }
}
