package cl.bookpointchile.inventario.controller;

import cl.bookpointchile.inventario.dto.*;
import cl.bookpointchile.inventario.service.InventarioService;
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
public class InventarioController {

    private final InventarioService inventarioService;

    @GetMapping("/{sucursalId}/producto/{productoId}")
    public ResponseEntity<InventarioResponseDTO> obtenerStock(
            @PathVariable Long sucursalId,
            @PathVariable Long productoId) {
        InventarioResponseDTO response = inventarioService.obtenerStock(sucursalId, productoId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/ajuste")
    public ResponseEntity<InventarioResponseDTO> registrarAjusteFisico(
            @Valid @RequestBody AjusteStockRequestDTO request) {
        InventarioResponseDTO response = inventarioService.registrarAjusteFisico(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/traslado")
    public ResponseEntity<InventarioResponseDTO> trasladarStock(
            @Valid @RequestBody TrasladoStockRequestDTO request) {
        InventarioResponseDTO response = inventarioService.trasladarStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/alertas")
    public ResponseEntity<List<InventarioResponseDTO>> obtenerAlertasReposicion() {
        List<InventarioResponseDTO> response = inventarioService.obtenerAlertasReposicion();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sucursal/{sucursalId}")
    public ResponseEntity<List<InventarioResponseDTO>> obtenerStockPorSucursal(
            @PathVariable Long sucursalId) {
        List<InventarioResponseDTO> response = inventarioService.obtenerStockPorSucursal(sucursalId);
        return ResponseEntity.ok(response);
    }

    // Endpoint clave consumido por ms-ventas vía FeignClient.
    // La disponibilidad es siempre relativa a una sucursal: no se suma el stock de toda la red.
    @GetMapping("/check-stock")
    public ResponseEntity<StockResponseDTO> checkStock(
            @RequestParam("sucursalId") Long sucursalId,
            @RequestParam("productoId") Long productoId,
            @RequestParam("cantidad") Integer cantidad) {
        StockResponseDTO response = inventarioService.verificarDisponibilidad(sucursalId, productoId, cantidad);
        return ResponseEntity.ok(response);
    }

    // Endpoint síncrono consumido por ms-ventas al confirmar una venta: descuenta, en un solo
    // paso, el stock de todas las líneas en la sucursal donde se realizó la compra.
    @PostMapping("/descuento")
    public ResponseEntity<Void> descontarStock(@Valid @RequestBody DescontarStockRequestDTO request) {
        inventarioService.descontarStockVenta(request);
        return ResponseEntity.ok().build();
    }
}
