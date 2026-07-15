package cl.bookpointchile.facturacion.controller;

import cl.bookpointchile.facturacion.dto.DocumentoResponseDTO;
import cl.bookpointchile.facturacion.dto.EmitirDocumentoRequestDTO;
import cl.bookpointchile.facturacion.service.FacturacionService;
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
@RequestMapping("/api/facturacion")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Habilita interoperabilidad Frontend-Backend en patrones CSR
@Tag(name = "Facturación", description = "Emisión y consulta de documentos tributarios (boletas y facturas)")
public class FacturacionController {

    private final FacturacionService service;

    @Operation(summary = "Emitir un documento tributario",
            description = "Emite una boleta o factura asociada a una venta, validando previamente "
                    + "la existencia de la venta.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Documento emitido correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o venta inexistente", content = @Content)
    })
    @PostMapping("/emitir")
    public ResponseEntity<DocumentoResponseDTO> emitirDocumento(
            @Valid @RequestBody EmitirDocumentoRequestDTO request) {
        DocumentoResponseDTO response = service.emitirDocumento(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Obtener documento por folio de venta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documento encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe documento para ese folio de venta", content = @Content)
    })
    @GetMapping("/venta/{folioVenta}")
    public ResponseEntity<DocumentoResponseDTO> obtenerPorFolioVenta(
            @Parameter(description = "Folio de la venta asociada") @PathVariable String folioVenta) {
        DocumentoResponseDTO response = service.obtenerPorFolioVenta(folioVenta);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar todos los documentos tributarios")
    @GetMapping
    public ResponseEntity<List<DocumentoResponseDTO>> obtenerTodos() {
        List<DocumentoResponseDTO> response = service.obtenerTodos();
        return ResponseEntity.ok(response);
    }
}
