package cl.bookpointchile.promociones.controller;

import cl.bookpointchile.promociones.dto.CrearPromocionRequestDTO;
import cl.bookpointchile.promociones.dto.PromocionResponseDTO;
import cl.bookpointchile.promociones.service.PromocionesService;
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
@RequestMapping("/api/promociones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Habilita la interoperabilidad Frontend-Backend en patrones CSR
@Tag(name = "Promociones", description = "Administración y validación de promociones y códigos de descuento")
public class PromocionController {

    private final PromocionesService promocionesService;

    @Operation(summary = "Registrar una promoción")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Promoción registrada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    @PostMapping
    public ResponseEntity<PromocionResponseDTO> registrarPromocion(
            @Valid @RequestBody CrearPromocionRequestDTO request) {
        PromocionResponseDTO response = promocionesService.registrarPromocion(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Listar todas las promociones")
    @GetMapping
    public ResponseEntity<List<PromocionResponseDTO>> obtenerTodas() {
        List<PromocionResponseDTO> response = promocionesService.obtenerTodas();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Validar un código de promoción",
            description = "Verifica que el código exista y esté vigente; es consumido por ms-ventas "
                    + "al aplicar descuentos durante el checkout.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Promoción válida y vigente"),
            @ApiResponse(responseCode = "404", description = "Código inexistente o no vigente", content = @Content)
    })
    @GetMapping("/validar/{codigo}")
    public ResponseEntity<PromocionResponseDTO> validarPromocion(
            @Parameter(description = "Código de la promoción") @PathVariable String codigo) {
        PromocionResponseDTO response = promocionesService.validarPromocion(codigo);
        return ResponseEntity.ok(response);
    }
}
