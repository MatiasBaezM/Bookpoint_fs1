package cl.bookpointchile.bodega.controller;

import cl.bookpointchile.bodega.dto.CrearOrdenPickingRequestDTO;
import cl.bookpointchile.bodega.dto.OrdenPickingResponseDTO;
import cl.bookpointchile.bodega.dto.UbicacionRequestDTO;
import cl.bookpointchile.bodega.dto.UbicacionResponseDTO;
import cl.bookpointchile.bodega.service.BodegaService;
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

import java.util.List;

@RestController
@RequestMapping("/api/bodega")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Habilita la interoperabilidad Frontend-Backend en patrones CSR
@Tag(name = "Bodega", description = "Gestión de ubicaciones físicas y órdenes de picking")
public class BodegaController {

    private final BodegaService service;

    @Operation(summary = "Registrar una ubicación de bodega")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ubicación registrada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    @PostMapping("/ubicaciones")
    public ResponseEntity<UbicacionResponseDTO> registrarUbicacion(
            @Valid @RequestBody UbicacionRequestDTO request) {
        UbicacionResponseDTO response = service.registrarUbicacion(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Crear una orden de picking",
            description = "Crea una orden de picking asociada a una venta, verificando previamente "
                    + "la existencia de la venta y el stock regional disponible.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Orden de picking creada correctamente"),
            @ApiResponse(responseCode = "400", description = "Venta inexistente o stock insuficiente", content = @Content)
    })
    @PostMapping("/picking")
    public ResponseEntity<OrdenPickingResponseDTO> crearOrdenPicking(
            @Valid @RequestBody CrearOrdenPickingRequestDTO request) {
        OrdenPickingResponseDTO response = service.crearOrdenPicking(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar estado de una orden de picking")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe orden con ese ID", content = @Content)
    })
    @PutMapping("/picking/{id}/estado")
    public ResponseEntity<OrdenPickingResponseDTO> actualizarEstadoPicking(
            @Parameter(description = "ID de la orden de picking") @PathVariable Long id,
            @Parameter(description = "Nuevo estado de la orden") @RequestParam String estado) {
        OrdenPickingResponseDTO response = service.actualizarEstadoPicking(id, estado);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar ubicaciones de bodega")
    @GetMapping("/ubicaciones")
    public ResponseEntity<List<UbicacionResponseDTO>> obtenerUbicaciones() {
        return ResponseEntity.ok(service.obtenerUbicaciones());
    }

    @Operation(summary = "Listar órdenes de picking")
    @GetMapping("/picking")
    public ResponseEntity<List<OrdenPickingResponseDTO>> obtenerOrdenesPicking() {
        return ResponseEntity.ok(service.obtenerOrdenesPicking());
    }
}
