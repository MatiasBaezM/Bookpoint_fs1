package cl.bookpointchile.ventas.controller;

import cl.bookpointchile.ventas.dto.VentaRequestDTO;
import cl.bookpointchile.ventas.dto.VentaResponseDTO;
import cl.bookpointchile.ventas.service.VentaService;
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
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Permite la comunicación ágil con el Frontend bajo el patrón CSR
@Tag(name = "Ventas", description = "Registro y consulta de ventas")
public class VentaController {

    private final VentaService ventaService;

    @Operation(summary = "Registrar una venta",
            description = "Registra una venta validando usuario, promoción y stock disponible; "
                    + "descuenta el stock en la sucursal y gatilla la emisión del documento tributario.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Venta registrada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o stock insuficiente", content = @Content)
    })
    @PostMapping
    public ResponseEntity<VentaResponseDTO> registrarVenta(@Valid @RequestBody VentaRequestDTO request) {
        VentaResponseDTO response = ventaService.registrarVenta(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Obtener venta por folio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venta encontrada"),
            @ApiResponse(responseCode = "404", description = "No existe venta con ese folio", content = @Content)
    })
    @GetMapping("/{folio}")
    public ResponseEntity<VentaResponseDTO> obtenerVentaPorFolio(
            @Parameter(description = "Folio único de la venta") @PathVariable String folio) {
        VentaResponseDTO response = ventaService.obtenerVentaPorFolio(folio);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar todas las ventas")
    @GetMapping
    public ResponseEntity<List<VentaResponseDTO>> obtenerTodas() {
        List<VentaResponseDTO> response = ventaService.obtenerTodas();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar ventas de un usuario")
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<VentaResponseDTO>> obtenerVentasPorUsuario(
            @Parameter(description = "ID del usuario comprador") @PathVariable Long usuarioId) {
        List<VentaResponseDTO> response = ventaService.obtenerVentasPorUsuario(usuarioId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener venta por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venta encontrada"),
            @ApiResponse(responseCode = "404", description = "No existe venta con ese ID", content = @Content)
    })
    @GetMapping("/id/{id}")
    public ResponseEntity<VentaResponseDTO> obtenerVentaPorId(
            @Parameter(description = "ID interno de la venta") @PathVariable Long id) {
        VentaResponseDTO response = ventaService.obtenerVentaPorId(id);
        return ResponseEntity.ok(response);
    }
}
