package br.com.cmachado.parkingsystem.domain.model.parkingsession;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PeriodTest {

    @Test
    void testDurationCalculation() {
        LocalDateTime entry = LocalDateTime.of(2023, 10, 1, 10, 0);
        LocalDateTime exit = LocalDateTime.of(2023, 10, 1, 11, 30);

        Period period = Period.start(entry).end(exit);

        assertEquals(90, period.getDurationInMinutes());
    }

    @Test
    void testInvalidExitTime() {
        LocalDateTime entry = LocalDateTime.of(2023, 10, 1, 10, 0);
        LocalDateTime exit = LocalDateTime.of(2023, 10, 1, 9, 30);

        Period period = Period.start(entry);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> period.end(exit));
        assertEquals("Exit time cannot be before entry time", ex.getMessage());
    }
}
