package br.com.cmachado.parkingsystem.application.parkingsession.impl;

import br.com.cmachado.parkingsystem.application.revenue.RevenueService;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSession;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingsession.ParkingSessionStatus;
import br.com.cmachado.parkingsystem.domain.model.revenue.DailyRevenueRepository;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorRepository;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpot;
import br.com.cmachado.parkingsystem.domain.model.parkingspot.ParkingSpotRepository;
import br.com.cmachado.parkingsystem.fixtures.ParkingSpotFixture;
import br.com.cmachado.parkingsystem.fixtures.SectorFixture;
import br.com.cmachado.parkingsystem.fixtures.WebhookEventFixture;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.revenue.RevenueResponse;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookRestController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Throughput and integrity test under concurrent load. Drives {@code VEHICLES} full
 * ENTRY → PARKED → EXIT lifecycles in parallel and asserts the system both keeps up with a
 * minimum throughput budget and stays consistent: every session exits, every spot is freed,
 * and the daily revenue equals the sum of all charges (no lost optimistic-lock updates).
 *
 * <p>Not transactional: use-case transactions must commit so the {@code AFTER_COMMIT} async
 * revenue listener fires.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class WebhookPerformanceTest {

    private static final Logger logger = LoggerFactory.getLogger(WebhookPerformanceTest.class);

    private static final String SECTOR = "SEC-PERF";
    private static final LocalDateTime ENTRY = LocalDateTime.parse("2025-01-01T10:00:00");
    private static final LocalDateTime EXIT = LocalDateTime.parse("2025-01-01T12:00:00");
    private static final int VEHICLES = 50;
    /** Lower bound on sustained lifecycle throughput. Conservative to stay stable on CI. */
    private static final double MIN_LIFECYCLES_PER_SECOND = 5.0;

    @Autowired private WebhookRestController webhookController;
    @Autowired private RevenueService revenueService;
    @Autowired private SectorRepository sectorRepository;
    @Autowired private ParkingSpotRepository spotRepository;
    @Autowired private ParkingSessionRepository sessionRepository;
    @Autowired private DailyRevenueRepository dailyRevenueRepository;

    @BeforeEach
    void setUp() {
        cleanAll();
        sectorRepository.save(SectorFixture.aSector().withCode(SECTOR).withCapacity(VEHICLES).build());
        for (int i = 1; i <= VEHICLES; i++) {
            spotRepository.save(ParkingSpotFixture.aSpot().withExternalId(i).withSector(SECTOR).withLocation(i, i).build());
        }
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
    void sustainsThroughputAndStaysConsistentUnderLoad() throws Exception {
        // arrange
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < VEHICLES; i++) indices.add(i);
        AtomicLong elapsedNanos = new AtomicLong();
        long start = System.nanoTime();

        // act — each vehicle runs its full lifecycle on its own spot, all in parallel
        runConcurrently(indices, i -> {
            String plate = "PERF" + String.format("%04d", i);
            double coord = i + 1;
            enter(plate, ENTRY);
            park(plate, coord, coord);
            exit(plate, EXIT);
        });
        elapsedNanos.set(System.nanoTime() - start);

        double seconds = elapsedNanos.get() / 1_000_000_000.0;
        double throughput = VEHICLES / seconds;
        logger.info("Processed {} lifecycles in {} ms → {} lifecycles/s",
                VEHICLES, String.format("%.1f", seconds * 1000), String.format("%.1f", throughput));

        // assert — all sessions exited, all spots released
        List<ParkingSession> sessions = sessionRepository.findAll();
        assertEquals(VEHICLES, sessions.size(), "every vehicle must have a session");
        assertTrue(sessions.stream().allMatch(s -> s.getStatus() == ParkingSessionStatus.EXITED),
                "every session must be EXITED");
        long occupied = spotRepository.findAll().stream().filter(ParkingSpot::isOccupied).count();
        assertEquals(0, occupied, "every spot must be released after exit");

        // Integrity: revenue equals the sum of all charges — no lost updates under contention.
        BigDecimal expected = sumChargedAmounts();
        assertTrue(expected.signum() > 0, "setup should produce positive charges");
        BigDecimal actual = awaitRevenue(SECTOR, expected);
        assertEquals(expected, actual, "daily revenue must equal the sum of all charges");

        // Throughput budget.
        assertTrue(throughput >= MIN_LIFECYCLES_PER_SECOND,
                () -> "throughput " + throughput + " lifecycles/s below floor " + MIN_LIFECYCLES_PER_SECOND);
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
    private void runConcurrently(List<Integer> inputs, java.util.function.IntConsumer action)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(inputs.size());
        CountDownLatch begin = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(inputs.size());
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        for (Integer input : inputs) {
            pool.submit(() -> {
                try {
                    begin.await();
                    action.accept(input);
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    done.countDown();
                }
            });
        }

        begin.countDown();
        assertTrue(done.await(60, TimeUnit.SECONDS), "concurrent lifecycles timed out");
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

    private BigDecimal awaitRevenue(String sector, BigDecimal expected) {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        BigDecimal amount = BigDecimal.ZERO;
        while (System.nanoTime() < deadline) {
            RevenueResponse response = revenueService.getRevenue(EXIT.toLocalDate(), sector);
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
