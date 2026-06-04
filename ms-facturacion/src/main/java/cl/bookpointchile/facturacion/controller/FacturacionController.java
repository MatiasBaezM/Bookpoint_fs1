package cl.bookpointchile.facturacion.controller;

import cl.bookpointchile.facturacion.dto.DocumentoResponseDTO;
import cl.bookpointchile.facturacion.dto.EmitirDocumentoRequestDTO;
import cl.bookpointchile.facturacion.service.FacturacionService;
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
public class FacturacionController {

    private final FacturacionService service;

    @PostMapping("/emitir")
    public ResponseEntity<DocumentoResponseDTO> emitirDocumento(
            @Valid @RequestBody EmitirDocumentoRequestDTO request) {
        DocumentoResponseDTO response = service.emitirDocumento(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/venta/{folioVenta}")
    public ResponseEntity<DocumentoResponseDTO> obtenerPorFolioVenta(
            @PathVariable String folioVenta) {
        DocumentoResponseDTO response = service.obtenerPorFolioVenta(folioVenta);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentoResponseDTO>> obtenerTodos() {
        List<DocumentoResponseDTO> response = service.obtenerTodos();
        return ResponseEntity.ok(response);
    }
}
