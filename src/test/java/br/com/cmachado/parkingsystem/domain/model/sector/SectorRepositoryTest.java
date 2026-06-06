package br.com.cmachado.parkingsystem.domain.model.sector;

import br.com.cmachado.parkingsystem.domain.model.common.money.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class SectorRepositoryTest {

    @Autowired
    private SectorRepository sectorRepository;

    @Test
    void minBasePriceReturnsLowestAcrossSectors() {
        sectorRepository.save(sector("A", "20.00"));
        sectorRepository.save(sector("B", "5.50"));
        sectorRepository.save(sector("C", "12.00"));

        Optional<BigDecimal> min = sectorRepository.findMinBasePrice();

        assertTrue(min.isPresent());
        assertEquals(new BigDecimal("5.50"), min.get());
    }

    @Test
    void minBasePriceIsEmptyWhenNoSectors() {
        assertTrue(sectorRepository.findMinBasePrice().isEmpty());
    }

    private Sector sector(String code, String basePrice) {
        return Sector.register(SectorCode.of(code), Money.of(basePrice), 10,
                LocalTime.MIDNIGHT, LocalTime.of(23, 59), 1440);
    }
}
