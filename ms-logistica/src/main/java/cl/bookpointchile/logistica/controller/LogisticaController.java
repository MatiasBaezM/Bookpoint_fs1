package cl.bookpointchile.logistica.controller;

import cl.bookpointchile.logistica.dto.ActualizarEstadoRequestDTO;
import cl.bookpointchile.logistica.dto.CrearEnvioRequestDTO;
import cl.bookpointchile.logistica.dto.EnvioResponseDTO;
import cl.bookpointchile.logistica.service.LogisticaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logistica")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Habilita la interoperabilidad Frontend-Backend en patrones CSR
public class LogisticaController {

    private final LogisticaService logisticaService;

    @PostMapping("/envios")
    public ResponseEntity<EnvioResponseDTO> crearEnvio(
            @Valid @RequestBody CrearEnvioRequestDTO request) {
        EnvioResponseDTO response = logisticaService.crearEnvio(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/envios/{id}/estado")
    public ResponseEntity<EnvioResponseDTO> actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoRequestDTO request) {
        EnvioResponseDTO response = logisticaService.actualizarEstado(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/envios/venta/{ventaId}")
    public ResponseEntity<EnvioResponseDTO> obtenerEnvioPorVentaId(@PathVariable Long ventaId) {
        EnvioResponseDTO response = logisticaService.obtenerEnvioPorVentaId(ventaId);
        return ResponseEntity.ok(response);
    }
}
