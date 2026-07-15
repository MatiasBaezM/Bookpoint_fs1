package cl.bookpointchile.sucursales.controller;

import cl.bookpointchile.sucursales.dto.SucursalRequestDTO;
import cl.bookpointchile.sucursales.dto.SucursalResponseDTO;
import cl.bookpointchile.sucursales.service.SucursalService;
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
@RequestMapping("/api/sucursales")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Habilita la interoperabilidad Frontend-Backend en patrones CSR
@Tag(name = "Sucursales", description = "Administración de sucursales de la red")
public class SucursalController {

    private final SucursalService service;

    @Operation(summary = "Listar todas las sucursales")
    @GetMapping
    public ResponseEntity<List<SucursalResponseDTO>> obtenerTodas() {
        List<SucursalResponseDTO> response = service.obtenerTodas();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener sucursal por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sucursal encontrada"),
            @ApiResponse(responseCode = "404", description = "No existe sucursal con ese ID", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<SucursalResponseDTO> obtenerPorId(
            @Parameter(description = "ID de la sucursal") @PathVariable Long id) {
        SucursalResponseDTO response = service.obtenerPorId(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Crear una sucursal")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sucursal creada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    @PostMapping
    public ResponseEntity<SucursalResponseDTO> crearSucursal(
            @Valid @RequestBody SucursalRequestDTO request) {
        SucursalResponseDTO response = service.crearSucursal(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar una sucursal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sucursal actualizada correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe sucursal con ese ID", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<SucursalResponseDTO> actualizarSucursal(
            @Parameter(description = "ID de la sucursal") @PathVariable Long id,
            @Valid @RequestBody SucursalRequestDTO request) {
        SucursalResponseDTO response = service.actualizarSucursal(id, request);
        return ResponseEntity.ok(response);
    }
}
