package cl.bookpointchile.facturacion.service;

import cl.bookpointchile.facturacion.dto.DocumentoResponseDTO;
import cl.bookpointchile.facturacion.dto.EmitirDocumentoRequestDTO;

import java.util.List;

public interface FacturacionService {
    DocumentoResponseDTO emitirDocumento(EmitirDocumentoRequestDTO request);
    DocumentoResponseDTO obtenerPorFolioVenta(String folioVenta);
    List<DocumentoResponseDTO> obtenerTodos();
}
