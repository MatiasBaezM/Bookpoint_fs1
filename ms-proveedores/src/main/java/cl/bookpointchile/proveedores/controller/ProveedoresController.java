package cl.bookpointchile.proveedores.controller;

import cl.bookpointchile.proveedores.dto.CrearProveedorRequestDTO;
import cl.bookpointchile.proveedores.dto.OrdenCompraRequestDTO;
import cl.bookpointchile.proveedores.dto.OrdenCompraResponseDTO;
import cl.bookpointchile.proveedores.dto.ProveedorResponseDTO;
import cl.bookpointchile.proveedores.service.ProveedoresService;
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
public class ProveedoresController {

    private final ProveedoresService proveedoresService;

    @GetMapping
    public ResponseEntity<List<ProveedorResponseDTO>> obtenerTodosProveedores() {
        List<ProveedorResponseDTO> response = proveedoresService.obtenerTodosProveedores();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ProveedorResponseDTO> registrarProveedor(
            @Valid @RequestBody CrearProveedorRequestDTO request) {
        ProveedorResponseDTO response = proveedoresService.registrarProveedor(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/ordenes")
    public ResponseEntity<OrdenCompraResponseDTO> emitirOrdenCompra(
            @Valid @RequestBody OrdenCompraRequestDTO request) {
        OrdenCompraResponseDTO response = proveedoresService.emitirOrdenCompra(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/ordenes/{id}/recepcion")
    public ResponseEntity<OrdenCompraResponseDTO> registrarRecepcionMercaderia(
            @PathVariable Long id) {
        OrdenCompraResponseDTO response = proveedoresService.registrarRecepcionMercaderia(id);
        return ResponseEntity.ok(response);
    }
}
