package ec.udla.consulta.service;

import com.fasterxml.jackson.databind.JsonNode;
import ec.udla.consulta.exception.ApiExceptions.NoEncontradoException;
import ec.udla.consulta.exception.ApiExceptions.ReglaNegocioException;
import ec.udla.consulta.exception.ApiExceptions.ServicioExternoException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class SriService {

    private static final String CONTRIBUYENTE =
            "https://srienlinea.sri.gob.ec/sri-catastro-sujeto-servicio-internet/rest/ConsolidadoContribuyente";
    private static final String VEHICULO =
            "https://srienlinea.sri.gob.ec/sri-matriculacion-vehicular-recaudacion-servicio-internet"
                    + "/rest/BaseVehiculo/obtenerPorNumeroPlacaOPorNumeroCampvOPorNumeroCpn";

    private final RestClient client;

    public SriService(@Qualifier("sriClient") RestClient client) {
        this.client = client;
    }

    /** Paso 1: verifica si el RUC pertenece a un contribuyente del SRI. */
    public boolean existeContribuyente(String ruc) {
        try {
            String body = client.get()
                    .uri(CONTRIBUYENTE + "/existePorNumeroRuc?numeroRuc={ruc}", ruc)
                    .retrieve().body(String.class);
            return body != null && body.trim().equalsIgnoreCase("true");
        } catch (RestClientException e) {
            throw new ServicioExternoException("El servicio del SRI no esta disponible", e);
        }
    }

    /**
     * Regla: persona natural = tercer digito del RUC menor a 6
     * (6 = entidad publica, 9 = sociedad privada).
     */
    public void validarPersonaNatural(String ruc) {
        int tercer = Character.getNumericValue(ruc.charAt(2));
        if (tercer >= 6) {
            throw new ReglaNegocioException("El RUC no corresponde a una persona natural");
        }
    }

    /** Paso 2: datos del contribuyente. */
    public JsonNode obtenerContribuyente(String ruc) {
        try {
            JsonNode datos = client.get()
                    .uri(CONTRIBUYENTE + "/obtenerPorNumerosRuc?ruc={ruc}", ruc)
                    .retrieve().body(JsonNode.class);
            if (datos == null || datos.isEmpty()) {
                throw new NoEncontradoException("No se encontraron datos para el RUC " + ruc);
            }
            return datos;
        } catch (RestClientException e) {
            throw new ServicioExternoException("El servicio del SRI no esta disponible", e);
        }
    }

    /** Paso 3: datos del vehiculo por placa, CAMV o CPN. */
    public JsonNode obtenerVehiculo(String placa) {
        try {
            JsonNode datos = client.get()
                    .uri(VEHICULO + "?numeroPlacaCampvCpn={placa}", placa)
                    .retrieve().body(JsonNode.class);
            if (datos == null || datos.isEmpty()) {
                throw new NoEncontradoException("No se encontro el vehiculo " + placa);
            }
            return datos;
        } catch (RestClientException e) {
            throw new ServicioExternoException("El servicio del SRI no esta disponible", e);
        }
    }
}
