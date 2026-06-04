package cl.bookpointchile.facturacion.service;

import cl.bookpointchile.facturacion.dto.DocumentoResponseDTO;
import cl.bookpointchile.facturacion.dto.EmitirDocumentoRequestDTO;
import cl.bookpointchile.facturacion.exception.DatosFacturacionIncompletosException;
import cl.bookpointchile.facturacion.exception.DocumentoDuplicadoException;
import cl.bookpointchile.facturacion.exception.DocumentoNoEncontradoException;
import cl.bookpointchile.facturacion.model.DocumentoTributario;
import cl.bookpointchile.facturacion.repository.DocumentoTributarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacturacionServiceImpl implements FacturacionService {

    private final DocumentoTributarioRepository repository;

    @Override
    @Transactional
    public DocumentoResponseDTO emitirDocumento(EmitirDocumentoRequestDTO request) {
        String folioUpper = request.getFolioVenta().trim().toUpperCase();
        String tipoUpper = request.getTipoDocumento().trim().toUpperCase();
        String rutClean = request.getRutCliente().trim();

        log.info("Iniciando emisión de {} para el Folio de Venta: '{}'", tipoUpper, folioUpper);

        // 1. Validar duplicidad
        if (repository.existsByFolioVenta(folioUpper)) {
            log.warn("Intento fallido de emisión: El folio de venta '{}' ya tiene un documento emitido.", folioUpper);
            throw new DocumentoDuplicadoException("El folio de venta '" + request.getFolioVenta() + "' ya tiene un documento tributario emitido.");
        }

        // 2. Validar tipo de documento
        if (!"BOLETA".equals(tipoUpper) && !"FACTURA".equals(tipoUpper)) {
            log.warn("Validación fallida: Tipo de documento '{}' no es válido.", tipoUpper);
            throw new DatosFacturacionIncompletosException("El tipo de documento debe ser 'BOLETA' o 'FACTURA'.");
        }

        String razonSocial = null;
        String giro = null;

        // 3. Validar requisitos de Factura
        if ("FACTURA".equals(tipoUpper)) {
            if (request.getRazonSocial() == null || request.getRazonSocial().trim().isEmpty() ||
                request.getGiro() == null || request.getGiro().trim().isEmpty()) {
                log.warn("Validación fallida: Intento de emitir FACTURA sin Razón Social o Giro comercial.");
                throw new DatosFacturacionIncompletosException("Para emitir una FACTURA, la Razón Social y el Giro del negocio son campos obligatorios.");
            }
            razonSocial = request.getRazonSocial().trim().toUpperCase();
            giro = request.getGiro().trim().toUpperCase();
        }

        // 4. Cálculo matemático del IVA (19%) y total con precisión
        // En Chile, las transacciones se redondean a pesos enteros
        double neto = request.getMontoNeto();
        double iva = Math.round(neto * 0.19);
        double total = neto + iva;

        log.info("Cálculos fiscales completados. Neto: {}, IVA (19%): {}, Total: {}", neto, iva, total);

        DocumentoTributario nuevoDoc = DocumentoTributario.builder()
                .folioVenta(folioUpper)
                .rutCliente(rutClean)
                .razonSocial(razonSocial)
                .giro(giro)
                .tipoDocumento(tipoUpper)
                .montoNeto(neto)
                .montoIva(iva)
                .montoTotal(total)
                .fechaEmision(LocalDateTime.now())
                .build();

        DocumentoTributario guardado = repository.save(nuevoDoc);
        log.info("{} emitida de forma exitosa. ID: {}, Folio: '{}', Total: ${}", 
                tipoUpper, guardado.getId(), guardado.getFolioVenta(), guardado.getMontoTotal());

        return mapToResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoResponseDTO obtenerPorFolioVenta(String folioVenta) {
        String folioUpper = folioVenta.trim().toUpperCase();
        log.info("Buscando documento tributario para el folio de venta: '{}'", folioUpper);

        DocumentoTributario doc = repository.findByFolioVenta(folioUpper)
                .orElseThrow(() -> {
                    log.warn("Documento tributario no encontrado para el folio de venta: '{}'", folioUpper);
                    return new DocumentoNoEncontradoException("No se encontró ningún documento tributario emitido para el folio de venta '" + folioVenta + "'.");
                });

        return mapToResponse(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoResponseDTO> obtenerTodos() {
        log.info("Obteniendo todos los documentos tributarios emitidos.");
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private DocumentoResponseDTO mapToResponse(DocumentoTributario d) {
        return DocumentoResponseDTO.builder()
                .id(d.getId())
                .folioVenta(d.getFolioVenta())
                .rutCliente(d.getRutCliente())
                .razonSocial(d.getRazonSocial())
                .giro(d.getGiro())
                .tipoDocumento(d.getTipoDocumento())
                .montoNeto(d.getMontoNeto())
                .montoIva(d.getMontoIva())
                .montoTotal(d.getMontoTotal())
                .fechaEmision(d.getFechaEmision())
                .build();
    }
}
