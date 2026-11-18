package ec.udla.consulta.controller;

import com.fasterxml.jackson.databind.JsonNode;
import ec.udla.consulta.dto.Dtos.ContribuyenteResponse;
import ec.udla.consulta.dto.Dtos.LicenciaResultado;
import ec.udla.consulta.dto.Dtos.VerificarRequest;
import ec.udla.consulta.exception.ApiExceptions.NoEncontradoException;
import ec.udla.consulta.service.AntService;
import ec.udla.consulta.service.SriService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ConsultaController {

    private static final Logger log = LoggerFactory.getLogger(ConsultaController.class);

    private final SriService sri;
    private final AntService ant;

    public ConsultaController(SriService sri, AntService ant) {
        this.sri = sri;
        this.ant = ant;
    }

    /** Flujo: existe contribuyente -> es persona natural -> datos. */
    @PostMapping("/contribuyentes/verificar")
    public ContribuyenteResponse verificar(@Valid @RequestBody VerificarRequest req) {
        if (!sri.existeContribuyente(req.ruc())) {
            throw new NoEncontradoException("El RUC no corresponde a un contribuyente del SRI");
        }
        sri.validarPersonaNatural(req.ruc());
        JsonNode datos = sri.obtenerContribuyente(req.ruc());
        String cedula = req.ruc().substring(0, 10);
        log.info("Consulta de contribuyente realizada para cedula {}", cedula);
        return new ContribuyenteResponse(cedula, req.email(), datos);
    }

    @GetMapping("/vehiculos/{placa}")
    public JsonNode vehiculo(@PathVariable String placa) {
        String p = placa.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (p.length() < 6 || p.length() > 17) {
            throw new IllegalArgumentException("Placa, CAMV o CPN invalido");
        }
        return sri.obtenerVehiculo(p);
    }

    @GetMapping("/licencias/{cedula}")
    public LicenciaResultado licencia(@PathVariable String cedula) {
        if (!cedula.matches("\\d{10}")) {
            throw new IllegalArgumentException("La cedula debe tener 10 digitos");
        }
        return ant.consultarPuntos(cedula);
    }
}
