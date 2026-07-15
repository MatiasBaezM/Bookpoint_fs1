package cl.bookpointchile.proveedores.controller;

import cl.bookpointchile.proveedores.dto.CrearProveedorRequestDTO;
import cl.bookpointchile.proveedores.dto.OrdenCompraRequestDTO;
import cl.bookpointchile.proveedores.dto.OrdenCompraResponseDTO;
import cl.bookpointchile.proveedores.dto.ProveedorResponseDTO;
import cl.bookpointchile.proveedores.service.ProveedoresService;
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
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Habilita la interoperabilidad Frontend-Backend en patrones CSR
@Tag(name = "Proveedores", description = "Gestión de proveedores y órdenes de compra")
public class ProveedoresController {

    private final ProveedoresService proveedoresService;

    @Operation(summary = "Listar todos los proveedores")
    @GetMapping
    public ResponseEntity<List<ProveedorResponseDTO>> obtenerTodosProveedores() {
        List<ProveedorResponseDTO> response = proveedoresService.obtenerTodosProveedores();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Registrar un proveedor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proveedor registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    @PostMapping
    public ResponseEntity<ProveedorResponseDTO> registrarProveedor(
            @Valid @RequestBody CrearProveedorRequestDTO request) {
        ProveedorResponseDTO response = proveedoresService.registrarProveedor(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Emitir una orden de compra",
            description = "Emite una orden de compra a un proveedor con las líneas de productos solicitadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Orden de compra emitida correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o proveedor inexistente", content = @Content)
    })
    @PostMapping("/ordenes")
    public ResponseEntity<OrdenCompraResponseDTO> emitirOrdenCompra(
            @Valid @RequestBody OrdenCompraRequestDTO request) {
        OrdenCompraResponseDTO response = proveedoresService.emitirOrdenCompra(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Registrar recepción de mercadería",
            description = "Marca la orden de compra como recepcionada y actualiza el stock "
                    + "de los productos recibidos en el inventario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recepción registrada correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe orden de compra con ese ID", content = @Content)
    })
    @PutMapping("/ordenes/{id}/recepcion")
    public ResponseEntity<OrdenCompraResponseDTO> registrarRecepcionMercaderia(
            @Parameter(description = "ID de la orden de compra") @PathVariable Long id) {
        OrdenCompraResponseDTO response = proveedoresService.registrarRecepcionMercaderia(id);
        return ResponseEntity.ok(response);
    }
}
