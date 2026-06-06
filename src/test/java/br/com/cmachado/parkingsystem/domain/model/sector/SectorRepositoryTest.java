package br.com.cmachado.parkingsystem.domain.model.sector;

import br.com.cmachado.parkingsystem.fixtures.SectorFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
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
        // arrange
        sectorRepository.save(SectorFixture.aSector().withCode("A").withBasePrice("20.00").build());
        sectorRepository.save(SectorFixture.aSector().withCode("B").withBasePrice("5.50").build());
        sectorRepository.save(SectorFixture.aSector().withCode("C").withBasePrice("12.00").build());

        // act
        Optional<BigDecimal> min = sectorRepository.findMinBasePrice();

        // assert
        assertTrue(min.isPresent(), "a min must be present when sectors exist");
        assertEquals(new BigDecimal("5.50"), min.get(), "lowest base price across sectors");
    }

    @Test
    void minBasePriceIsEmptyWhenNoSectors() {
        // act / assert
        assertTrue(sectorRepository.findMinBasePrice().isEmpty(), "no sectors must yield an empty min");
    }
}
