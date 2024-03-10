package ma.globalperformance.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

	private static final String PALIERS_CACHE_NAME = "paliersCache";
	@Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        // Créer un ConcurrentMapCache avec le nom de cache souhaité
        ConcurrentMapCache cache = new ConcurrentMapCache(PALIERS_CACHE_NAME);

        List<ConcurrentMapCache> caches = new ArrayList();

     // Ajouter les caches à la liste
        caches.add(cache);
        // Ajouter le cache au SimpleCacheManager
        cacheManager.setCaches(caches); 
        
        return cacheManager;
    }
}
