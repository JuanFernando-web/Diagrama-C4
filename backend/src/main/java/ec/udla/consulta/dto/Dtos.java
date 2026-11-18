package ec.udla.consulta.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class Dtos {

    private Dtos() {}

    public record VerificarRequest(
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "\\d{13}", message = "El RUC debe tener 13 digitos") String ruc) {}

    public record ContribuyenteResponse(String cedula, String email, JsonNode datos) {}

    /** Dato que se guarda en cache: puntos de la licencia segun la ANT. */
    public record LicenciaInfo(String cedula, Integer puntos) {}

    /**
     * Respuesta al front.
     * origen: ANT_EN_VIVO | CACHE_FRESCO | CACHE_RESPALDO
     * consultadoEn: epoch millis de cuando la ANT respondio por ultima vez.
     */
    public record LicenciaResultado(LicenciaInfo licencia, String origen, long consultadoEn) {}

    /** Envoltorio guardado en Redis. */
    public record CacheEntry<T>(T data, long cachedAt) {}
}
