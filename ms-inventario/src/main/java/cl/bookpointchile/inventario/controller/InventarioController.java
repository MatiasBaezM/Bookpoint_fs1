package cl.bookpointchile.inventario.controller;

import cl.bookpointchile.inventario.dto.*;
import cl.bookpointchile.inventario.service.InventarioService;
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
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Habilita la integración fluida con clientes CSR
@Tag(name = "Inventario", description = "Control de stock por sucursal: consultas, ajustes, traslados y alertas")
public class InventarioController {

    private final InventarioService inventarioService;

    @Operation(summary = "Obtener stock de un producto en una sucursal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe registro de inventario", content = @Content)
    })
    @GetMapping("/{sucursalId}/producto/{productoId}")
    public ResponseEntity<InventarioResponseDTO> obtenerStock(
            @Parameter(description = "ID de la sucursal") @PathVariable Long sucursalId,
            @Parameter(description = "ID del producto") @PathVariable Long productoId) {
        InventarioResponseDTO response = inventarioService.obtenerStock(sucursalId, productoId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Registrar ajuste físico de stock",
            description = "Ajusta el stock de un producto en una sucursal tras un conteo físico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ajuste registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    @PutMapping("/ajuste")
    public ResponseEntity<InventarioResponseDTO> registrarAjusteFisico(
            @Valid @RequestBody AjusteStockRequestDTO request) {
        InventarioResponseDTO response = inventarioService.registrarAjusteFisico(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Trasladar stock entre sucursales",
            description = "Descuenta stock de la sucursal de origen y lo suma en la de destino.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Traslado registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o stock insuficiente en origen", content = @Content)
    })
    @PostMapping("/traslado")
    public ResponseEntity<InventarioResponseDTO> trasladarStock(
            @Valid @RequestBody TrasladoStockRequestDTO request) {
        InventarioResponseDTO response = inventarioService.trasladarStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar alertas de reposición",
            description = "Devuelve los registros de inventario cuyo stock está bajo el umbral de reposición.")
    @GetMapping("/alertas")
    public ResponseEntity<List<InventarioResponseDTO>> obtenerAlertasReposicion() {
        List<InventarioResponseDTO> response = inventarioService.obtenerAlertasReposicion();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar stock de una sucursal")
    @GetMapping("/sucursal/{sucursalId}")
    public ResponseEntity<List<InventarioResponseDTO>> obtenerStockPorSucursal(
            @Parameter(description = "ID de la sucursal") @PathVariable Long sucursalId) {
        List<InventarioResponseDTO> response = inventarioService.obtenerStockPorSucursal(sucursalId);
        return ResponseEntity.ok(response);
    }

    // Endpoint clave consumido por ms-ventas vía FeignClient.
    // La disponibilidad es siempre relativa a una sucursal: no se suma el stock de toda la red.
    @Operation(summary = "Verificar disponibilidad de stock",
            description = "Verifica si hay stock suficiente de un producto en una sucursal. "
                    + "Consumido por ms-ventas vía FeignClient; la disponibilidad es siempre "
                    + "relativa a una sucursal, no a la red completa.")
    @GetMapping("/check-stock")
    public ResponseEntity<StockResponseDTO> checkStock(
            @Parameter(description = "ID de la sucursal") @RequestParam("sucursalId") Long sucursalId,
            @Parameter(description = "ID del producto") @RequestParam("productoId") Long productoId,
            @Parameter(description = "Cantidad requerida") @RequestParam("cantidad") Integer cantidad) {
        StockResponseDTO response = inventarioService.verificarDisponibilidad(sucursalId, productoId, cantidad);
        return ResponseEntity.ok(response);
    }

    // Endpoint síncrono consumido por ms-ventas al confirmar una venta: descuenta, en un solo
    // paso, el stock de todas las líneas en la sucursal donde se realizó la compra.
    @Operation(summary = "Descontar stock por venta",
            description = "Descuenta en un solo paso el stock de todas las líneas de una venta "
                    + "en la sucursal donde se realizó la compra. Consumido por ms-ventas al confirmar la venta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock descontado correctamente"),
            @ApiResponse(responseCode = "400", description = "Stock insuficiente para alguna línea", content = @Content)
    })
    @PostMapping("/descuento")
    public ResponseEntity<Void> descontarStock(@Valid @RequestBody DescontarStockRequestDTO request) {
        inventarioService.descontarStockVenta(request);
        return ResponseEntity.ok().build();
    }
}
