package br.com.cmachado.parkingsystem.application.parkingsession;

import br.com.cmachado.parkingsystem.application.parkingsession.webhook.mediator.WebhookEventMediator;
import br.com.cmachado.parkingsystem.application.revenue.RevenueService;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorRepository;
import br.com.cmachado.parkingsystem.domain.model.revenue.DailyRevenueRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.violations.GarageFullException;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionRepository;
import br.com.cmachado.parkingsystem.fixtures.ParkingSpotFixture;
import br.com.cmachado.parkingsystem.fixtures.SectorFixture;
import br.com.cmachado.parkingsystem.fixtures.WebhookEventFixture;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.revenue.RevenueResponse;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * End-to-end flow test. Not transactional on purpose: the revenue update runs on an
 * {@code AFTER_COMMIT} async listener, so the use-case transactions must actually commit.
 * Data is cleaned between tests instead of rolled back.
 */
@SpringBootTest
@ActiveProfiles("test")
class WebhookEventMediatorIntegrationTest {

    private static final String SECTOR = "SEC-A";

    @Autowired
    private WebhookEventMediator webhookEventMediator;

    @Autowired
    private RevenueService revenueService;

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
        sectorRepository.save(SectorFixture.aSector().withCode(SECTOR).build());
        spotRepository.save(ParkingSpotFixture.aSpot().withExternalId(1L).withSector(SECTOR).withLocation(10.0, 10.0).build());
        spotRepository.save(ParkingSpotFixture.aSpot().withExternalId(2L).withSector(SECTOR).withLocation(20.0, 20.0).build());
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
    void completeVehicleFlowChargesAndRecordsRevenue() {
        // arrange
        String plate = "ABC1234";

        // act — full lifecycle through the webhook mediator
        webhookEventMediator.handle(WebhookEventFixture.anEntry().withPlate(plate).at(LocalDateTime.now().minusHours(2)).build());
        webhookEventMediator.handle(WebhookEventFixture.aParked().withPlate(plate).atLocation(10.0, 10.0).build());
        webhookEventMediator.handle(WebhookEventFixture.anExit().withPlate(plate).at(LocalDateTime.now()).build());

        // assert — revenue updates asynchronously after the exit commits.
        // Stayed 2h, base 10. Spot released before charge calc → occupancy 0/2 = 0% → 10% discount: 20 * 0.90
        BigDecimal amount = awaitRevenue(SECTOR);
        assertEquals(new BigDecimal("18.00"), amount, "2h stay at 0% occupancy bills 18.00");
    }

    @Test
    void entryRejectedWhenGarageFull() {
        // arrange — fill both spots so the garage reaches 100% occupancy
        parkVehicle("AAA1111", 10.0, 10.0);
        parkVehicle("BBB2222", 20.0, 20.0);
        long countBefore = sessionRepository.count();

        // act / assert
        assertThrows(GarageFullException.class,
                () -> webhookEventMediator.handle(WebhookEventFixture.anEntry().withPlate("CCC3333").at(LocalDateTime.now()).build()),
                "entry into a full garage must be rejected");
        assertEquals(countBefore, sessionRepository.count(), "rejected entry must not be stored");
    }

    private void parkVehicle(String plate, double lat, double lng) {
        webhookEventMediator.handle(WebhookEventFixture.anEntry().withPlate(plate).at(LocalDateTime.now()).build());
        webhookEventMediator.handle(WebhookEventFixture.aParked().withPlate(plate).atLocation(lat, lng).build());
    }

    /** Polls the revenue endpoint until the async update lands or a timeout is reached. */
    private BigDecimal awaitRevenue(String sector) {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        BigDecimal amount = BigDecimal.ZERO;
        while (System.nanoTime() < deadline) {
            RevenueResponse response = revenueService.getRevenue(LocalDate.now(), sector);
            assertNotNull(response, "revenue response must not be null");
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
