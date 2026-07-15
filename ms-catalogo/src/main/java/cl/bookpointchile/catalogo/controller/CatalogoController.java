package cl.bookpointchile.catalogo.controller;

import cl.bookpointchile.catalogo.dto.*;
import cl.bookpointchile.catalogo.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Catálogo", description = "Gestión del catálogo de productos y reseñas")
public class CatalogoController {

    private final CatalogoService catalogoService;

    @Operation(summary = "Buscar productos con filtros",
            description = "Búsqueda paginada de productos filtrando opcionalmente por autor, "
                    + "editorial, categoría y rango de precios.")
    @GetMapping("/productos")
    public ResponseEntity<Page<ProductoResponseDTO>> buscarProductos(
            @Parameter(description = "Autor del libro") @RequestParam(required = false) String autor,
            @Parameter(description = "Editorial del libro") @RequestParam(required = false) String editorial,
            @Parameter(description = "Categoría del producto") @RequestParam(required = false) String categoria,
            @Parameter(description = "Precio mínimo") @RequestParam(required = false) BigDecimal precioMin,
            @Parameter(description = "Precio máximo") @RequestParam(required = false) BigDecimal precioMax,
            @Parameter(description = "Número de página (desde 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size) {

        Page<ProductoResponseDTO> response = catalogoService.buscarProductosConFiltros(
                autor, editorial, categoria, precioMin, precioMax, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener producto por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe producto con ese ID", content = @Content)
    })
    @GetMapping("/productos/{id}")
    public ResponseEntity<ProductoResponseDTO> obtenerProductoPorId(
            @Parameter(description = "ID del producto") @PathVariable Long id) {
        ProductoResponseDTO response = catalogoService.obtenerProductoPorId(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Registrar un producto",
            description = "Registra un nuevo producto en el catálogo validando su unicidad.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o producto duplicado", content = @Content)
    })
    @PostMapping("/productos")
    public ResponseEntity<ProductoResponseDTO> registrarProducto(
            @Valid @RequestBody ProductoRegistroRequestDTO request) {
        ProductoResponseDTO response = catalogoService.registrarProducto(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Agregar reseña a un producto")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reseña registrada correctamente"),
            @ApiResponse(responseCode = "404", description = "No existe producto con ese ID", content = @Content)
    })
    @PostMapping("/productos/{id}/resenas")
    public ResponseEntity<ResenaResponseDTO> agregarResena(
            @Parameter(description = "ID del producto reseñado") @PathVariable Long id,
            @Valid @RequestBody ResenaRequestDTO request) {
        ResenaResponseDTO response = catalogoService.agregarResena(id, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
