package cl.bookpointchile.logistica.controller;

import cl.bookpointchile.logistica.dto.ActualizarEstadoRequestDTO;
import cl.bookpointchile.logistica.dto.CrearEnvioRequestDTO;
import cl.bookpointchile.logistica.dto.EnvioResponseDTO;
import cl.bookpointchile.logistica.service.LogisticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logistica")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Habilita la interoperabilidad Frontend-Backend en patrones CSR
@Tag(name = "Logística", description = "Creación y seguimiento de envíos asociados a ventas")
public class LogisticaController {

    private final LogisticaService logisticaService;

    @Operation(summary = "Crear un envío",
            description = "Crea un envío asociado a una venta con su dirección de despacho.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Envío creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    @PostMapping("/envios")
    public ResponseEntity<EnvioResponseDTO> crearEnvio(
            @Valid @RequestBody CrearEnvioRequestDTO request) {
        EnvioResponseDTO response = logisticaService.crearEnvio(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar estado de un envío")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe envío con ese ID", content = @Content)
    })
    @PutMapping("/envios/{id}/estado")
    public ResponseEntity<EnvioResponseDTO> actualizarEstado(
            @Parameter(description = "ID del envío") @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoRequestDTO request) {
        EnvioResponseDTO response = logisticaService.actualizarEstado(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener envío por ID de venta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Envío encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe envío para esa venta", content = @Content)
    })
    @GetMapping("/envios/venta/{ventaId}")
    public ResponseEntity<EnvioResponseDTO> obtenerEnvioPorVentaId(
            @Parameter(description = "ID de la venta asociada") @PathVariable Long ventaId) {
        EnvioResponseDTO response = logisticaService.obtenerEnvioPorVentaId(ventaId);
        return ResponseEntity.ok(response);
    }
}
