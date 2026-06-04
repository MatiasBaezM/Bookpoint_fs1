package cl.bookpointchile.sucursales.controller;

import cl.bookpointchile.sucursales.dto.SucursalRequestDTO;
import cl.bookpointchile.sucursales.dto.SucursalResponseDTO;
import cl.bookpointchile.sucursales.service.SucursalService;
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
public class SucursalController {

    private final SucursalService service;

    @GetMapping
    public ResponseEntity<List<SucursalResponseDTO>> obtenerTodas() {
        List<SucursalResponseDTO> response = service.obtenerTodas();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SucursalResponseDTO> obtenerPorId(@PathVariable Long id) {
        SucursalResponseDTO response = service.obtenerPorId(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<SucursalResponseDTO> crearSucursal(
            @Valid @RequestBody SucursalRequestDTO request) {
        SucursalResponseDTO response = service.crearSucursal(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SucursalResponseDTO> actualizarSucursal(
            @PathVariable Long id,
            @Valid @RequestBody SucursalRequestDTO request) {
        SucursalResponseDTO response = service.actualizarSucursal(id, request);
        return ResponseEntity.ok(response);
    }
}
