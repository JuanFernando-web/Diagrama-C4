package ec.udla.consulta.service;

import ec.udla.consulta.dto.Dtos.CacheEntry;
import ec.udla.consulta.dto.Dtos.LicenciaInfo;
import ec.udla.consulta.dto.Dtos.LicenciaResultado;
import ec.udla.consulta.exception.ApiExceptions.ServicioExternoException;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Patron: Cache-Aside + Fallback (stale-if-error).
 *
 *  1. Si hay dato en cache y es "fresco"  -> se devuelve sin tocar la ANT.
 *  2. Si no, se consulta la ANT (con timeout).
 *       - Respuesta valida  -> se guarda en Redis y se devuelve.
 *       - Falla / invalida  -> se devuelve el ultimo dato valido del cache (respaldo).
 *  3. Sin cache y sin ANT -> 503.
 */
@Service
public class AntService {

    private static final Logger log = LoggerFactory.getLogger(AntService.class);
    private static final String URL =
            "https://consultaweb.ant.gob.ec/PortalWEB/paginas/clientes/clp_grid_citaciones.jsp"
                    + "?ps_tipo_identificacion=CED&ps_identificacion={cedula}&ps_placa=";
    // AJUSTAR tras inspeccionar el HTML real de la ANT (ver README).
    private static final Pattern PUNTOS = Pattern.compile("(?i)puntos[^0-9]{0,60}(\\d{1,2})");

    private final RestClient client;
    private final CacheService cache;
    private final long freshMillis;
    private final Duration ttl;

    public AntService(@Qualifier("antClient") RestClient client,
                      CacheService cache,
                      @Value("${app.ant.cache.fresh-seconds}") long freshSeconds,
                      @Value("${app.ant.cache.ttl-days}") long ttlDays) {
        this.client = client;
        this.cache = cache;
        this.freshMillis = freshSeconds * 1000;
        this.ttl = Duration.ofDays(ttlDays);
    }

    public LicenciaResultado consultarPuntos(String cedula) {
        String key = "ant:licencia:" + cedula;
        Optional<CacheEntry<LicenciaInfo>> cached = cache.get(key, LicenciaInfo.class);

        long ahora = System.currentTimeMillis();
        if (cached.isPresent() && ahora - cached.get().cachedAt() < freshMillis) {
            CacheEntry<LicenciaInfo> c = cached.get();
            return new LicenciaResultado(c.data(), "CACHE_FRESCO", c.cachedAt());
        }

        try {
            LicenciaInfo info = consultarEnVivo(cedula);
            cache.put(key, info, ttl);               // solo se cachea una respuesta valida
            return new LicenciaResultado(info, "ANT_EN_VIVO", System.currentTimeMillis());
        } catch (Exception e) {
            log.warn("ANT no disponible para {}: {}", cedula, e.getMessage());
            if (cached.isPresent()) {
                CacheEntry<LicenciaInfo> c = cached.get();
                return new LicenciaResultado(c.data(), "CACHE_RESPALDO", c.cachedAt());
            }
            throw new ServicioExternoException(
                    "La ANT no esta disponible y no hay informacion previa en cache", e);
        }
    }

    private LicenciaInfo consultarEnVivo(String cedula) {
        String html = client.get().uri(URL, cedula).retrieve().body(String.class);
        if (html == null || html.isBlank()) {
            throw new IllegalStateException("Respuesta vacia de la ANT");
        }
        String texto = Jsoup.parse(html).text();
        Matcher m = PUNTOS.matcher(texto);
        if (!m.find()) {
            // No es una respuesta "adecuada": no se cachea.
            throw new IllegalStateException("La respuesta de la ANT no contiene los puntos de licencia");
        }
        return new LicenciaInfo(cedula, Integer.valueOf(m.group(1)));
    }
}
