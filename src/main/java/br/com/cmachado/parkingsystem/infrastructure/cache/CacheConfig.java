package br.com.cmachado.parkingsystem.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Caches sector reference data, which the simulator syncs once at startup and is never mutated
 * at runtime. Reads on the pricing hot path ({@code findByCode}, {@code findMinBasePrice}) hit
 * the cache instead of the database.
 *
 * <p>Correctness comes from eviction on garage initialization (see
 * {@code GarageInitializerServiceImpl}); the TTL is only defense-in-depth and an ops tuning knob.</p>
 *
 * <p>Set {@code app.cache.enabled=false} to swap in a {@link NoOpCacheManager}: the
 * {@code @Cacheable}/{@code @CacheEvict} annotations stay in place but do nothing, so every read
 * goes straight to the database.</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String SECTORS = "sectors";
    public static final String SECTOR_MIN_BASE_PRICE = "sectorMinBasePrice";

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    private final boolean enabled;
    private final Duration sectorTtl;
    private final long sectorMaxSize;

    public CacheConfig(@Value("${app.cache.enabled:true}") boolean enabled,
                       @Value("${app.cache.sector.ttl:12h}") Duration sectorTtl,
                       @Value("${app.cache.sector.max-size:64}") long sectorMaxSize) {
        this.enabled = enabled;
        this.sectorTtl = sectorTtl;
        this.sectorMaxSize = sectorMaxSize;
    }

    @Bean
    public CacheManager cacheManager() {
        if (!enabled) {
            logger.info("Application cache disabled (app.cache.enabled=false); using no-op cache manager.");
            return new NoOpCacheManager();
        }

        var cacheManager = new CaffeineCacheManager(SECTORS, SECTOR_MIN_BASE_PRICE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(sectorTtl)
                .maximumSize(sectorMaxSize)
                .recordStats());
        return cacheManager;
    }
}
