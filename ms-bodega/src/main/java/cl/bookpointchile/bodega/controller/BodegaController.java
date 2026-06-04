package cl.bookpointchile.bodega.controller;

import cl.bookpointchile.bodega.dto.CrearOrdenPickingRequestDTO;
import cl.bookpointchile.bodega.dto.OrdenPickingResponseDTO;
import cl.bookpointchile.bodega.dto.UbicacionRequestDTO;
import cl.bookpointchile.bodega.dto.UbicacionResponseDTO;
import cl.bookpointchile.bodega.service.BodegaService;
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
public class BodegaController {

    private final BodegaService service;

    @PostMapping("/ubicaciones")
    public ResponseEntity<UbicacionResponseDTO> registrarUbicacion(
            @Valid @RequestBody UbicacionRequestDTO request) {
        UbicacionResponseDTO response = service.registrarUbicacion(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/picking")
    public ResponseEntity<OrdenPickingResponseDTO> crearOrdenPicking(
            @Valid @RequestBody CrearOrdenPickingRequestDTO request) {
        OrdenPickingResponseDTO response = service.crearOrdenPicking(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/picking/{id}/estado")
    public ResponseEntity<OrdenPickingResponseDTO> actualizarEstadoPicking(
            @PathVariable Long id,
            @RequestParam String estado) {
        OrdenPickingResponseDTO response = service.actualizarEstadoPicking(id, estado);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ubicaciones")
    public ResponseEntity<List<UbicacionResponseDTO>> obtenerUbicaciones() {
        return ResponseEntity.ok(service.obtenerUbicaciones());
    }

    @GetMapping("/picking")
    public ResponseEntity<List<OrdenPickingResponseDTO>> obtenerOrdenesPicking() {
        return ResponseEntity.ok(service.obtenerOrdenesPicking());
    }
}
