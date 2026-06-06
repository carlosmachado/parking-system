package br.com.cmachado.parkingsystem.domain.model.parkingsession;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PeriodTest {

    @Test
    void durationIsMinutesBetweenEntryAndExit() {
        // arrange
        LocalDateTime entry = LocalDateTime.of(2023, 10, 1, 10, 0);
        LocalDateTime exit = LocalDateTime.of(2023, 10, 1, 11, 30);

        // act
        Period period = Period.start(entry).end(exit);

        // assert
        assertEquals(90, period.getDurationInMinutes(), "10:00 → 11:30 is 90 minutes");
    }

    @Test
    void exitBeforeEntryIsRejected() {
        // arrange
        LocalDateTime entry = LocalDateTime.of(2023, 10, 1, 10, 0);
        LocalDateTime exit = LocalDateTime.of(2023, 10, 1, 9, 30);
        Period period = Period.start(entry);

        // act / assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> period.end(exit),
                "exit before entry must be rejected");
        assertEquals("Exit time cannot be before entry time", ex.getMessage(), "rejection message");
    }
}
