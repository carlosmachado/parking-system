package br.com.cmachado.parkingsystem.infrastructure.cache;

import br.com.cmachado.parkingsystem.application.garage.GarageInitializerService;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorCode;
import br.com.cmachado.parkingsystem.domain.model.sector.SectorRepository;
import br.com.cmachado.parkingsystem.fixtures.SectorFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that sector reads are served from the Caffeine cache and that garage initialization
 * evicts it. The DB row is deleted out-of-band (a delete is not cached) to prove the second read
 * never touches the database.
 */
@SpringBootTest
@ActiveProfiles("test")
class SectorCacheTest {

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private GarageInitializerService garageInitializerService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCacheAndData() {
        sectorRepository.deleteAll();
        cacheManager.getCache(CacheConfig.SECTORS).clear();
    }

    @AfterEach
    void cleanUp() {
        sectorRepository.deleteAll();
        cacheManager.getCache(CacheConfig.SECTORS).clear();
    }

    @Test
    void secondReadIsServedFromCacheNotDatabase() {
        // arrange — persist a sector, prime the cache, then wipe the table (delete is not cached)
        sectorRepository.saveAndFlush(SectorFixture.aSector().withCode("CACHE-A").withBasePrice("10.00").build());
        sectorRepository.findByCode(SectorCode.of("CACHE-A"));
        sectorRepository.deleteAll();

        // act — DB is now empty; a cache miss would return empty
        var fromCache = sectorRepository.findByCode(SectorCode.of("CACHE-A"));

        // assert — value survives because it came from the cache
        assertTrue(fromCache.isPresent(), "second read must be served from cache, not the empty table");
    }

    @Test
    void initializeGarageEvictsSectorCache() {
        // arrange — prime the cache, then empty the table
        sectorRepository.saveAndFlush(SectorFixture.aSector().withCode("CACHE-B").withBasePrice("10.00").build());
        sectorRepository.findByCode(SectorCode.of("CACHE-B"));
        sectorRepository.deleteAll();

        // act — initialization evicts the sector cache (null config returns normally, evict still fires)
        garageInitializerService.initializeGarage(null);

        // assert — next read misses the cache and hits the now-empty table
        var afterEvict = sectorRepository.findByCode(SectorCode.of("CACHE-B"));
        assertTrue(afterEvict.isEmpty(), "garage initialization must evict the sector cache");
    }
}
