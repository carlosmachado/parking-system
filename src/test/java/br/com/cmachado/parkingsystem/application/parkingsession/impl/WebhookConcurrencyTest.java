package br.com.cmachado.parkingsystem.application.parkingsession.impl;

import br.com.cmachado.parkingsystem.application.revenue.RevenueService;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorRepository;
import br.com.cmachado.parkingsystem.domain.model.revenue.DailyRevenueRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionStatus;
import br.com.cmachado.parkingsystem.fixtures.ParkingSpotFixture;
import br.com.cmachado.parkingsystem.fixtures.SectorFixture;
import br.com.cmachado.parkingsystem.fixtures.WebhookEventFixture;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.revenue.RevenueResponse;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookRestController;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the optimistic-lock retry hardening that ships with virtual threads.
 *
 * <p>Not transactional: use-case transactions must actually commit so the {@code AFTER_COMMIT}
 * async revenue listener fires and so concurrent threads observe each other's writes.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class WebhookConcurrencyTest {

    private static final String SECTOR = "SEC-A";

    @Autowired
    private WebhookRestController webhookController;

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

    /**
     * Concurrent exits in the same sector on the same day all contend on a single
     * {@code DailyRevenue} row. The retry loop must ensure no increment is lost: the stored
     * total equals the sum of the individual charges.
     */
    @Test
    void concurrentExitsDoNotLoseRevenue() throws Exception {
        // arrange — 10 vehicles parked across 10 spots in one sector
        int n = 10;
        sectorRepository.save(SectorFixture.aSector().withCode(SECTOR).withCapacity(20).build());
        for (int i = 1; i <= n; i++) {
            spotRepository.save(ParkingSpotFixture.aSpot().withExternalId(i).withSector(SECTOR).withLocation(i, i).build());
        }
        List<String> plates = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String plate = "REV" + String.format("%04d", i);
            plates.add(plate);
            enter(plate, LocalDateTime.now().minusHours(2));
            park(plate, (double) (i + 1), (double) (i + 1));
        }

        // act — fire all exits at once so the async revenue handlers race on the same row
        runConcurrently(plates, plate -> exit(plate, LocalDateTime.now()));

        // assert
        BigDecimal expected = sumChargedAmounts();
        assertTrue(expected.signum() > 0, "test setup should produce a positive charge");
        BigDecimal actual = awaitRevenue(SECTOR, expected);
        assertEquals(expected, actual, "daily revenue must equal the sum of all charges");
    }

    /**
     * Two PARKED events aimed at the same nearest spot race on {@code ParkingSpot.version}. The loser
     * is retried by the controller, re-reads, and takes the other spot — both vehicles end up
     * parked on distinct spots with no exception leaking.
     */
    @Test
    void concurrentParkedAssignsDistinctSpots() throws Exception {
        // arrange — two free spots, two entered vehicles
        sectorRepository.save(SectorFixture.aSector().withCode(SECTOR).build());
        spotRepository.save(ParkingSpotFixture.aSpot().withExternalId(1L).withSector(SECTOR).withLocation(10.0, 10.0).build());
        spotRepository.save(ParkingSpotFixture.aSpot().withExternalId(2L).withSector(SECTOR).withLocation(20.0, 20.0).build());
        String p1 = "PRK0001";
        String p2 = "PRK0002";
        enter(p1, LocalDateTime.now());
        enter(p2, LocalDateTime.now());

        // act — each vehicle parks at its own spot location, concurrently
        runConcurrently(List.of(p1, p2), plate -> park(plate, plate.equals(p1) ? 10.0 : 20.0, plate.equals(p1) ? 10.0 : 20.0));

        // assert
        List<ParkingSession> sessions = sessionRepository.findAll();
        assertEquals(2, sessions.size(), "both vehicles must have a session");
        assertTrue(sessions.stream().allMatch(s -> s.getStatus() == ParkingSessionStatus.PARKED),
                "both vehicles must be parked");
        assertNotEquals(sessions.get(0).getSpotId(), sessions.get(1).getSpotId(),
                "vehicles must occupy distinct spots");
        long occupiedSpots = spotRepository.findAll().stream()
                .filter(ParkingSpot::isOccupied)
                .count();
        assertEquals(2, occupiedSpots, "exactly two spots must be occupied");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void enter(String plate, LocalDateTime entryTime) {
        webhookController.handleEvent(WebhookEventFixture.anEntry().withPlate(plate).at(entryTime).build());
    }

    private void park(String plate, Double lat, Double lng) {
        webhookController.handleEvent(WebhookEventFixture.aParked().withPlate(plate).atLocation(lat, lng).build());
    }

    private void exit(String plate, LocalDateTime exitTime) {
        webhookController.handleEvent(WebhookEventFixture.anExit().withPlate(plate).at(exitTime).build());
    }

    /** Runs the action for every input simultaneously and rethrows the first failure. */
    private void runConcurrently(List<String> inputs, java.util.function.Consumer<String> action)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(inputs.size());
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(inputs.size());
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        for (String input : inputs) {
            pool.submit(() -> {
                try {
                    start.await();
                    action.accept(input);
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(20, TimeUnit.SECONDS), "concurrent actions timed out");
        pool.shutdownNow();

        if (!failures.isEmpty()) {
            throw new AssertionError("concurrent action failed: " + failures.get(0), failures.get(0));
        }
    }

    private BigDecimal sumChargedAmounts() {
        BigDecimal sum = BigDecimal.ZERO;
        for (ParkingSession session : sessionRepository.findAll()) {
            if (session.getStatus() == ParkingSessionStatus.EXITED && session.getAmountCharged() != null) {
                sum = sum.add(session.getAmountCharged().getAmount());
            }
        }
        return sum;
    }

    /** Polls the revenue endpoint until it reaches {@code expected} or a timeout is reached. */
    private BigDecimal awaitRevenue(String sector, BigDecimal expected) {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        BigDecimal amount = BigDecimal.ZERO;
        while (System.nanoTime() < deadline) {
            RevenueResponse response = revenueService.getRevenue(LocalDate.now(), sector);
            amount = response.getAmount();
            if (amount.compareTo(expected) == 0) {
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
