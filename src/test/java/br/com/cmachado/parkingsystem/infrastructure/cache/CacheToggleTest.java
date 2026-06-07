package br.com.cmachado.parkingsystem.infrastructure.cache;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * With {@code app.cache.enabled=false} the cache manager must be a {@link NoOpCacheManager},
 * so {@code @Cacheable} reads fall through to the database.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.cache.enabled=false")
class CacheToggleTest {

    @Autowired
    private CacheManager cacheManager;

    @Test
    void disabledCacheUsesNoOpManager() {
        assertInstanceOf(NoOpCacheManager.class, cacheManager,
                "app.cache.enabled=false must yield a no-op cache manager");
    }
}
