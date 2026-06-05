package br.com.cmachado.parkingsystem.application.webhook.impl;

import br.com.cmachado.parkingsystem.application.revenue.RevenueApplicationService;
import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import br.com.cmachado.parkingsystem.domain.model.garage.Sector;
import br.com.cmachado.parkingsystem.domain.model.garage.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.garage.SectorRepository;
import br.com.cmachado.parkingsystem.domain.model.revenue.DailyRevenueRepository;
import br.com.cmachado.parkingsystem.domain.model.spot.GeoLocation;
import br.com.cmachado.parkingsystem.domain.model.spot.Spot;
import br.com.cmachado.parkingsystem.domain.model.spot.SpotRepository;
import br.com.cmachado.parkingsystem.domain.model.vehicle.VehicleEvent;
import br.com.cmachado.parkingsystem.domain.model.vehicle.VehicleEventRepository;
import br.com.cmachado.parkingsystem.domain.model.vehicle.VehicleEventStatus;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.revenue.RevenueResponse;
import br.com.cmachado.parkingsystem.presentation.controllers.rest.webhook.WebhookEventRequest;
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
    private RevenueApplicationService revenueService;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private SpotRepository spotRepository;

    @Autowired
    private VehicleEventRepository vehicleEventRepository;

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
        vehicleEventRepository.deleteAll();
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
        SectorCode code = new SectorCode(SECTOR);
        sectorRepository.save(new Sector(code, Money.of(10.0), 20));
        int n = 10;
        for (int i = 1; i <= n; i++) {
            spotRepository.save(Spot.register((long) i, code, new GeoLocation(i, i)));
        }

        List<String> plates = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String plate = "REV" + String.format("%04d", i);
            plates.add(plate);
            enter(plate, LocalDateTime.now().minusHours(2));
            park(plate, null, null);
        }

        // Fire all exits at once so the async revenue handlers race on the same row.
        runConcurrently(plates, plate -> exit(plate, LocalDateTime.now()));

        BigDecimal expected = sumChargedAmounts();
        assertTrue(expected.signum() > 0, "test setup should produce a positive charge");

        BigDecimal actual = awaitRevenue(SECTOR, expected);
        assertEquals(expected, actual, "daily revenue must equal the sum of all charges");
    }

    /**
     * Two PARKED events aimed at the same nearest spot race on {@code Spot.version}. The loser
     * is retried by the controller, re-reads, and takes the other spot — both vehicles end up
     * parked on distinct spots with no exception leaking.
     */
    @Test
    void concurrentParkedAssignsDistinctSpots() throws Exception {
        SectorCode code = new SectorCode(SECTOR);
        sectorRepository.save(new Sector(code, Money.of(10.0), 10));
        spotRepository.save(Spot.register(1L, code, new GeoLocation(10.0, 10.0)));
        spotRepository.save(Spot.register(2L, code, new GeoLocation(20.0, 20.0)));

        String p1 = "PRK0001";
        String p2 = "PRK0002";
        enter(p1, LocalDateTime.now());
        enter(p2, LocalDateTime.now());

        // Identical location → both compute the same nearest spot → forces the race.
        runConcurrently(List.of(p1, p2), plate -> park(plate, 10.0, 10.0));

        List<VehicleEvent> events = vehicleEventRepository.findAll();
        assertEquals(2, events.size());
        assertTrue(events.stream().allMatch(e -> e.getStatus() == VehicleEventStatus.PARKED),
                "both vehicles must be parked");
        assertNotEquals(events.get(0).getSpotId(), events.get(1).getSpotId(),
                "vehicles must occupy distinct spots");
        assertEquals(2, spotRepository.countByOccupiedTrue());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void enter(String plate, LocalDateTime entryTime) {
        WebhookEventRequest req = new WebhookEventRequest();
        req.setLicensePlate(plate);
        req.setEventType("ENTRY");
        req.setEntryTime(entryTime.toString());
        webhookController.handleEvent(req);
    }

    private void park(String plate, Double lat, Double lng) {
        WebhookEventRequest req = new WebhookEventRequest();
        req.setLicensePlate(plate);
        req.setEventType("PARKED");
        req.setLat(lat);
        req.setLng(lng);
        webhookController.handleEvent(req);
    }

    private void exit(String plate, LocalDateTime exitTime) {
        WebhookEventRequest req = new WebhookEventRequest();
        req.setLicensePlate(plate);
        req.setEventType("EXIT");
        req.setExitTime(exitTime.toString());
        webhookController.handleEvent(req);
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
        for (VehicleEvent event : vehicleEventRepository.findAll()) {
            if (event.getStatus() == VehicleEventStatus.EXITED && event.getAmountCharged() != null) {
                sum = sum.add(event.getAmountCharged().getAmount());
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
