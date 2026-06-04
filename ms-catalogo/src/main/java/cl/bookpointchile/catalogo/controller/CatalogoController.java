package cl.bookpointchile.catalogo.controller;

import cl.bookpointchile.catalogo.dto.*;
import cl.bookpointchile.catalogo.service.CatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Facilita la interoperabilidad del frontend bajo patrón CSR
public class CatalogoController {

    private final CatalogoService catalogoService;

    @GetMapping("/productos")
    public ResponseEntity<Page<ProductoResponseDTO>> buscarProductos(
            @RequestParam(required = false) String autor,
            @RequestParam(required = false) String editorial,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
            
        Page<ProductoResponseDTO> response = catalogoService.buscarProductosConFiltros(
                autor, editorial, categoria, precioMin, precioMax, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/productos/{id}")
    public ResponseEntity<ProductoResponseDTO> obtenerProductoPorId(@PathVariable Long id) {
        ProductoResponseDTO response = catalogoService.obtenerProductoPorId(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/productos")
    public ResponseEntity<ProductoResponseDTO> registrarProducto(
            @Valid @RequestBody ProductoRegistroRequestDTO request) {
        ProductoResponseDTO response = catalogoService.registrarProducto(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/productos/{id}/resenas")
    public ResponseEntity<ResenaResponseDTO> agregarResena(
            @PathVariable Long id,
            @Valid @RequestBody ResenaRequestDTO request) {
        ResenaResponseDTO response = catalogoService.agregarResena(id, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
