package ec.udla.consulta.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.udla.consulta.dto.Dtos.CacheEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Adaptador hacia Redis Cloud. Si Redis falla NO se rompe la consulta:
 * simplemente se comporta como cache vacio.
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public CacheService(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    public <T> Optional<CacheEntry<T>> get(String key, Class<T> type) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) return Optional.empty();
            JavaType jt = mapper.getTypeFactory().constructParametricType(CacheEntry.class, type);
            CacheEntry<T> entry = mapper.readValue(json, jt);
            return Optional.of(entry);
        } catch (Exception e) {
            log.warn("No se pudo leer el cache para {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public <T> void put(String key, T data, Duration ttl) {
        try {
            String json = mapper.writeValueAsString(new CacheEntry<>(data, System.currentTimeMillis()));
            redis.opsForValue().set(key, json, ttl);
        } catch (Exception e) {
            log.warn("No se pudo escribir el cache para {}: {}", key, e.getMessage());
        }
    }
}
