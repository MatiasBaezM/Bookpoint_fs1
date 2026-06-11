package cl.bookpointchile.ventas.service;

import cl.bookpointchile.ventas.client.FacturacionClient;
import cl.bookpointchile.ventas.client.InventarioClient;
import cl.bookpointchile.ventas.client.PromocionClient;
import cl.bookpointchile.ventas.dto.*;
import cl.bookpointchile.ventas.exception.InsufficientStockException;
import cl.bookpointchile.ventas.exception.InvalidSaleException;
import cl.bookpointchile.ventas.exception.ResourceNotFoundException;
import cl.bookpointchile.ventas.model.*;
import cl.bookpointchile.ventas.repository.VentaRepository;
import cl.bookpointchile.ventas.event.VentaCreadaEvent;
import cl.bookpointchile.ventas.event.DetalleVentaEvent;
import cl.bookpointchile.ventas.config.RabbitMQConfig;
import feign.FeignException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VentaServiceImpl implements VentaService {

    private static final String RUT_CLIENTE_GENERICO = "66666666-6";

    private final VentaRepository ventaRepository;
    private final RabbitTemplate rabbitTemplate;
    private final InventarioClient inventarioClient;
    private final PromocionClient promocionClient;
    private final FacturacionClient facturacionClient;

    @Override
    @Transactional
    public VentaResponseDTO registrarVenta(VentaRequestDTO request) {
        log.info("Iniciando registro de venta. Tipo: {}, Canal: {}", 
                request.getTipoVenta(), request.getTipoVenta() == TipoVenta.PRESENCIAL ? "Caja" : "Online");

        // 1. Validaciones de Negocio Específicas
        if (request.getTipoVenta() == TipoVenta.PRESENCIAL && 
                (request.getAsistenteNombre() == null || request.getAsistenteNombre().trim().isEmpty())) {
            log.error("Error de validación: Venta presencial sin nombre de asistente.");
            throw new InvalidSaleException("Para ventas presenciales en caja es obligatorio ingresar el nombre del asistente de ventas.");
        }

        // 2. Verificación de Stock en Tiempo Real (Síncrono vía Feign con ms-inventario)
        for (DetalleVentaRequestDTO item : request.getDetalles()) {
            StockResponseDTO stock = inventarioClient.checkStock(item.getProductoId(), item.getCantidad());
            if (!stock.isDisponible()) {
                log.warn("Stock insuficiente para el producto ID {}. Disponible: {}, Solicitado: {}",
                        item.getProductoId(), stock.getStockActual(), item.getCantidad());
                throw new InsufficientStockException("Stock insuficiente para el producto '" + item.getProductoNombre() +
                        "' (ID " + item.getProductoId() + "). Disponible: " + stock.getStockActual() +
                        ", Solicitado: " + item.getCantidad());
            }
        }

        // 3. Cálculo de Subtotales y Totales
        BigDecimal subtotal = BigDecimal.ZERO;
        for (DetalleVentaRequestDTO item : request.getDetalles()) {
            BigDecimal itemSubtotal = item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad()));
            subtotal = subtotal.add(itemSubtotal);
        }

        // 4. Aplicar Descuentos o Convenios Estudiantiles / Cupones Web
        BigDecimal descuento = BigDecimal.ZERO;
        TipoDescuento tipoDescuento = TipoDescuento.NINGUNO;
        String codigo = request.getCodigoDescuento();

        if (codigo != null && !codigo.trim().isEmpty()) {
            String codigoClean = codigo.trim().toUpperCase();
            PromocionResponseDTO promocion;
            try {
                promocion = promocionClient.validarPromocion(codigoClean);
            } catch (FeignException.NotFound | FeignException.BadRequest e) {
                log.warn("Código de descuento no válido intentado: {}", codigo);
                throw new InvalidSaleException("El cupón o convenio estudiantil '" + codigo + "' ingresado no es válido.");
            } catch (FeignException e) {
                log.error("No fue posible validar el código de descuento '{}' con ms-promociones: {}", codigo, e.getMessage());
                throw new InvalidSaleException("No fue posible validar el código de descuento '" + codigo + "'. Intente nuevamente más tarde.");
            }

            descuento = subtotal.multiply(BigDecimal.valueOf(promocion.getPorcentajeDescuento()))
                    .divide(new BigDecimal("100"));
            tipoDescuento = codigoClean.equals("CONVENIO_ESTUDIANTIL") ? TipoDescuento.CONVENIO_ESTUDIANTIL : TipoDescuento.CUPON;
            log.info("Descuento del {}% aplicado por código '{}' (tipo: {})",
                    promocion.getPorcentajeDescuento(), codigoClean, tipoDescuento);
        }

        // Redondear descuento y calcular total
        descuento = descuento.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(descuento).setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        // 5. Generar Entidad Venta y Detalles (Bidireccional)
        String folioUnico = "BP-" + request.getTipoVenta().name().substring(0, 3) + "-" + 
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Venta venta = Venta.builder()
                .folio(folioUnico)
                .fecha(LocalDateTime.now())
                .tipoVenta(request.getTipoVenta())
                .clienteNombre(request.getClienteNombre())
                .clienteRut(request.getClienteRut())
                .asistenteNombre(request.getTipoVenta() == TipoVenta.PRESENCIAL ? request.getAsistenteNombre() : null)
                .subtotal(subtotal)
                .descuentoAplicado(descuento)
                .tipoDescuento(tipoDescuento)
                .codigoDescuento(tipoDescuento != TipoDescuento.NINGUNO ? codigo.trim().toUpperCase() : null)
                .estado(EstadoVenta.PENDIENTE)
                .total(total)
                .build();

        for (DetalleVentaRequestDTO item : request.getDetalles()) {
            BigDecimal itemSubtotal = item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad()));
            DetalleVenta detalle = DetalleVenta.builder()
                    .productoId(item.getProductoId())
                    .productoNombre(item.getProductoNombre())
                    .cantidad(item.getCantidad())
                    .precioUnitario(item.getPrecioUnitario())
                    .subtotal(itemSubtotal)
                    .build();
            venta.addDetalle(detalle);
        }

        // 6. Guardar en Base de Datos (Estado PENDIENTE)
        Venta ventaGuardada = ventaRepository.save(venta);
        log.info("Venta guardada con éxito en la base de datos (Estado: PENDIENTE). Folio: {}, ID de Venta: {}", 
                ventaGuardada.getFolio(), ventaGuardada.getId());

        // 7. Emisión de Documento Tributario (Boleta) vía Feign con ms-facturacion (best-effort, no bloqueante)
        try {
            EmitirDocumentoRequestDTO facturaRequest = EmitirDocumentoRequestDTO.builder()
                    .folioVenta(ventaGuardada.getFolio())
                    .rutCliente(ventaGuardada.getClienteRut() != null && !ventaGuardada.getClienteRut().trim().isEmpty()
                            ? ventaGuardada.getClienteRut() : RUT_CLIENTE_GENERICO)
                    .tipoDocumento("BOLETA")
                    .montoNeto(ventaGuardada.getTotal().doubleValue())
                    .build();
            DocumentoResponseDTO documento = facturacionClient.emitirDocumento(facturaRequest);
            log.info("Documento tributario emitido para folio {}: ID {}", ventaGuardada.getFolio(), documento.getId());
        } catch (Exception e) {
            log.warn("No fue posible emitir el documento tributario para la venta {}: {}", ventaGuardada.getFolio(), e.getMessage());
        }

        // 8. Emitir Evento VentaCreada para MS-Inventario
        List<DetalleVentaEvent> detallesEvent = ventaGuardada.getDetalles().stream()
                .map(d -> DetalleVentaEvent.builder()
                        .productoId(d.getProductoId())
                        .cantidad(d.getCantidad())
                        .build())
                .collect(Collectors.toList());

        VentaCreadaEvent evento = VentaCreadaEvent.builder()
                .ventaId(ventaGuardada.getId())
                .folio(ventaGuardada.getFolio())
                .detalles(detallesEvent)
                .build();

        log.info("Enviando evento VentaCreada a RabbitMQ para folio: {}", ventaGuardada.getFolio());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_VENTAS, RabbitMQConfig.ROUTING_KEY_VENTA_CREADA, evento);

        return mapToResponse(ventaGuardada);
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponseDTO obtenerVentaPorFolio(String folio) {
        log.info("Buscando venta con folio: {}", folio);
        Venta venta = ventaRepository.findByFolio(folio)
                .orElseThrow(() -> {
                    log.error("Venta no encontrada con folio: {}", folio);
                    return new ResourceNotFoundException("La venta con el folio '" + folio + "' no existe.");
                });
        return mapToResponse(venta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponseDTO> obtenerTodas() {
        log.info("Obteniendo listado de todas las ventas.");
        return ventaRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Mapper manual Helper para mantener el diseño CSR libre de acoplamientos pesados
    private VentaResponseDTO mapToResponse(Venta venta) {
        List<DetalleVentaResponseDTO> detalleDTOs = venta.getDetalles().stream()
                .map(d -> DetalleVentaResponseDTO.builder()
                        .id(d.getId())
                        .productoId(d.getProductoId())
                        .productoNombre(d.getProductoNombre())
                        .cantidad(d.getCantidad())
                        .precioUnitario(d.getPrecioUnitario())
                        .subtotal(d.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return VentaResponseDTO.builder()
                .id(venta.getId())
                .folio(venta.getFolio())
                .fecha(venta.getFecha())
                .tipoVenta(venta.getTipoVenta())
                .estado(venta.getEstado())
                .clienteNombre(venta.getClienteNombre())
                .clienteRut(venta.getClienteRut())
                .asistenteNombre(venta.getAsistenteNombre())
                .subtotal(venta.getSubtotal())
                .descuentoAplicado(venta.getDescuentoAplicado())
                .tipoDescuento(venta.getTipoDescuento())
                .codigoDescuento(venta.getCodigoDescuento())
                .total(venta.getTotal())
                .detalles(detalleDTOs)
                .build();
    }
}
