package br.com.cmachado.parkingsystem.presentation.controllers.rest.revenue;

import br.com.cmachado.parkingsystem.application.revenue.RevenueService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Exposes the accumulated revenue for a day, optionally narrowed to a single sector.
 */
@RestController
@RequestMapping("/revenue")
public class RevenueRestController {

    private final RevenueService revenueService;

    public RevenueRestController(RevenueService revenueService) {
        this.revenueService = revenueService;
    }

    /**
     * Returns the revenue for the given day. Both query params are optional:
     * {@code date} defaults to today, and when {@code sector} is omitted the totals of all
     * sectors are aggregated.
     *
     * @param date   day to report on (ISO {@code yyyy-MM-dd}); defaults to today
     * @param sector sector code to filter by; all sectors when blank
     */
    @GetMapping
    public ResponseEntity<RevenueResponse> getRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String sector) {

        LocalDate targetDate = date != null ? date : LocalDate.now();

        if (sector != null && !sector.isBlank()) {
            RevenueResponse response = revenueService.getRevenue(targetDate, sector.trim().toUpperCase());
            return ResponseEntity.ok(response);
        }

        // No sector specified → aggregate all sectors for the day
        RevenueResponse response = revenueService.getRevenueAllSectors(targetDate);
        return ResponseEntity.ok(response);
    }
}
