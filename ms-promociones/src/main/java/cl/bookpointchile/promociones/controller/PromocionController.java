package cl.bookpointchile.promociones.controller;

import cl.bookpointchile.promociones.dto.CrearPromocionRequestDTO;
import cl.bookpointchile.promociones.dto.PromocionResponseDTO;
import cl.bookpointchile.promociones.service.PromocionesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promociones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Habilita la interoperabilidad Frontend-Backend en patrones CSR
public class PromocionController {

    private final PromocionesService promocionesService;

    @PostMapping
    public ResponseEntity<PromocionResponseDTO> registrarPromocion(
            @Valid @RequestBody CrearPromocionRequestDTO request) {
        PromocionResponseDTO response = promocionesService.registrarPromocion(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PromocionResponseDTO>> obtenerTodas() {
        List<PromocionResponseDTO> response = promocionesService.obtenerTodas();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validar/{codigo}")
    public ResponseEntity<PromocionResponseDTO> validarPromocion(
            @PathVariable String codigo) {
        PromocionResponseDTO response = promocionesService.validarPromocion(codigo);
        return ResponseEntity.ok(response);
    }
}
