package cl.bookpointchile.facturacion.service;

import cl.bookpointchile.facturacion.client.UsuariosClient;
import cl.bookpointchile.facturacion.client.VentasClient;
import cl.bookpointchile.facturacion.dto.DocumentoResponseDTO;
import cl.bookpointchile.facturacion.dto.EmitirDocumentoRequestDTO;
import cl.bookpointchile.facturacion.dto.UsuarioResponseDTO;
import cl.bookpointchile.facturacion.dto.VentaResponseDTO;
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
    private final VentasClient ventasClient;
    private final UsuariosClient usuariosClient;

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

        // 2. Validar existencia y consistencia de la venta (pedido)
        try {
            VentaResponseDTO venta = ventasClient.obtenerVentaPorFolio(folioUpper);
            if (venta == null) {
                throw new DatosFacturacionIncompletosException("La venta con el folio '" + request.getFolioVenta() + "' no existe.");
            }
            // Validar que el monto neto coincida con el total de la venta (Neto = total / 1.19)
            double netoCalculado = Math.round(venta.getTotal() / 1.19);
            if (Math.abs(netoCalculado - request.getMontoNeto()) > 1.0) { // Tolerancia por redondeo
                log.error("Validación fallida: Monto neto de la petición ({}) no coincide con el calculado de la venta ({}).",
                        request.getMontoNeto(), netoCalculado);
                throw new DatosFacturacionIncompletosException("El monto neto ingresado (" + request.getMontoNeto() + 
                        ") no coincide con el total de la venta registrada.");
            }
        } catch (DatosFacturacionIncompletosException e) {
            throw e;
        } catch (feign.FeignException.NotFound e) {
            log.error("Venta no encontrada en ms-ventas con folio: '{}'", folioUpper);
            throw new DatosFacturacionIncompletosException("La venta con el folio '" + request.getFolioVenta() + "' no existe en el sistema de ventas.");
        } catch (Exception e) {
            log.warn("No fue posible comunicarse con ms-ventas para validar la venta. Continuando en modo degradado: {}", e.getMessage());
        }

        // 3. Validar existencia del cliente (si no es el RUT genérico)
        String rutGenerico = "66666666-6";
        if (!rutGenerico.equals(rutClean)) {
            try {
                UsuarioResponseDTO usuario = usuariosClient.obtenerUsuarioPorRut(rutClean);
                if (usuario == null || !"ACTIVO".equalsIgnoreCase(usuario.getEstado())) {
                    throw new DatosFacturacionIncompletosException("El cliente con RUT " + request.getRutCliente() + " no está registrado o no se encuentra activo.");
                }
            } catch (DatosFacturacionIncompletosException e) {
                throw e;
            } catch (feign.FeignException.NotFound e) {
                log.error("Cliente no encontrado en ms-usuarios con RUT: '{}'", rutClean);
                throw new DatosFacturacionIncompletosException("El cliente con RUT " + request.getRutCliente() + " no existe en el registro de usuarios.");
            } catch (Exception e) {
                log.warn("No fue posible comunicarse con ms-usuarios para validar el cliente. Continuando en modo degradado: {}", e.getMessage());
            }
        }

        // 4. Validar tipo de documento
        if (!"BOLETA".equals(tipoUpper) && !"FACTURA".equals(tipoUpper)) {
            log.warn("Validación fallida: Tipo de documento '{}' no es válido.", tipoUpper);
            throw new DatosFacturacionIncompletosException("El tipo de documento debe ser 'BOLETA' o 'FACTURA'.");
        }

        String razonSocial = null;
        String giro = null;

        // 5. Validar requisitos de Factura
        if ("FACTURA".equals(tipoUpper)) {
            if (request.getRazonSocial() == null || request.getRazonSocial().trim().isEmpty() ||
                request.getGiro() == null || request.getGiro().trim().isEmpty()) {
                log.warn("Validación fallida: Intento de emitir FACTURA sin Razón Social o Giro comercial.");
                throw new DatosFacturacionIncompletosException("Para emitir una FACTURA, la Razón Social y el Giro del negocio son campos obligatorios.");
            }
            razonSocial = request.getRazonSocial().trim().toUpperCase();
            giro = request.getGiro().trim().toUpperCase();
        }

        // 6. Cálculo matemático del IVA (19%) y total con precisión
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
