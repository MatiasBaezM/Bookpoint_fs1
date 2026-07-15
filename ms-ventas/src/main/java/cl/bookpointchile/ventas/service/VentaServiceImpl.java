package cl.bookpointchile.ventas.service;

import cl.bookpointchile.ventas.client.FacturacionClient;
import cl.bookpointchile.ventas.client.InventarioClient;
import cl.bookpointchile.ventas.client.PromocionClient;
import cl.bookpointchile.ventas.client.UsuarioClient;
import cl.bookpointchile.ventas.dto.*;
import cl.bookpointchile.ventas.exception.InsufficientStockException;
import cl.bookpointchile.ventas.exception.InvalidSaleException;
import cl.bookpointchile.ventas.exception.ResourceNotFoundException;
import cl.bookpointchile.ventas.model.*;
import cl.bookpointchile.ventas.repository.VentaRepository;
import cl.bookpointchile.ventas.event.VentaRegistradaInternaEvent;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final InventarioClient inventarioClient;
    private final PromocionClient promocionClient;
    private final FacturacionClient facturacionClient;
    private final UsuarioClient usuarioClient;
    private final ApplicationEventPublisher eventPublisher;

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

        // 2. Resolución del cliente registrado (opcional vía usuarioId → ms-usuarios)
        String clienteNombreResuelto = request.getClienteNombre();
        String clienteRutResuelto = request.getClienteRut();

        if (request.getUsuarioId() != null) {
            try {
                UsuarioResponseDTO usuario = usuarioClient.obtenerUsuarioPorId(request.getUsuarioId());
                if (!"ACTIVO".equalsIgnoreCase(usuario.getEstado())) {
                    throw new InvalidSaleException("El usuario con ID " + request.getUsuarioId() + " no está activo.");
                }
                // Los datos del usuario registrado tienen prioridad sobre los campos manuales
                clienteNombreResuelto = usuario.getNombre();
                clienteRutResuelto = usuario.getRut();
                log.info("Cliente registrado resuelto: {} (RUT: {})", clienteNombreResuelto, clienteRutResuelto);
            } catch (InvalidSaleException e) {
                throw e;
            } catch (FeignException.NotFound e) {
                throw new InvalidSaleException("No existe un usuario registrado con ID " + request.getUsuarioId() + ".");
            } catch (FeignException e) {
                log.error("No fue posible verificar el usuario ID {} con ms-usuarios: {}", request.getUsuarioId(), e.getMessage());
                throw new InvalidSaleException("No fue posible verificar el usuario. Intente nuevamente más tarde.");
            }
        }

        // 3. Verificación de Stock en Tiempo Real en la sucursal de la venta (Síncrono vía Feign con ms-inventario)
        for (DetalleVentaRequestDTO item : request.getDetalles()) {
            StockResponseDTO stock = inventarioClient.checkStock(
                    request.getSucursalId(), item.getProductoId(), item.getCantidad());
            if (!stock.isDisponible()) {
                log.warn("Stock insuficiente en la sucursal ID {} para el producto ID {}. Disponible: {}, Solicitado: {}",
                        request.getSucursalId(), item.getProductoId(), stock.getStockActual(), item.getCantidad());
                throw new InsufficientStockException("Stock insuficiente para el producto '" + item.getProductoNombre() +
                        "' (ID " + item.getProductoId() + ") en la sucursal ID " + request.getSucursalId() +
                        ". Disponible: " + stock.getStockActual() + ", Solicitado: " + item.getCantidad());
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

        // Validar tipo de documento solicitado
        String docType = request.getTipoDocumento() != null ? request.getTipoDocumento().trim().toUpperCase() : "BOLETA";
        if (!"BOLETA".equals(docType) && !"FACTURA".equals(docType)) {
            throw new InvalidSaleException("El tipo de documento debe ser 'BOLETA' o 'FACTURA'.");
        }
        if ("FACTURA".equals(docType)) {
            if (request.getRazonSocial() == null || request.getRazonSocial().trim().isEmpty() ||
                request.getGiro() == null || request.getGiro().trim().isEmpty()) {
                throw new InvalidSaleException("Para emitir una FACTURA, la Razón Social y el Giro del negocio son campos obligatorios.");
            }
        }

        // 5. Generar Entidad Venta y Detalles (Bidireccional)
        String folioUnico = "BP-" + request.getTipoVenta().name().substring(0, 3) + "-" + 
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Venta venta = Venta.builder()
                .folio(folioUnico)
                .fecha(LocalDateTime.now())
                .tipoVenta(request.getTipoVenta())
                .sucursalId(request.getSucursalId())
                .usuarioId(request.getUsuarioId())
                .clienteNombre(clienteNombreResuelto)
                .clienteRut(clienteRutResuelto)
                .asistenteNombre(request.getTipoVenta() == TipoVenta.PRESENCIAL ? request.getAsistenteNombre() : null)
                .subtotal(subtotal)
                .descuentoAplicado(descuento)
                .tipoDescuento(tipoDescuento)
                .codigoDescuento(tipoDescuento != TipoDescuento.NINGUNO ? codigo.trim().toUpperCase() : null)
                .estado(EstadoVenta.PENDIENTE)
                .tipoDocumento(docType)
                .razonSocial("FACTURA".equals(docType) ? request.getRazonSocial().trim().toUpperCase() : null)
                .giro("FACTURA".equals(docType) ? request.getGiro().trim().toUpperCase() : null)
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

        // 7. Publicar evento interno para emisión del documento tributario tras confirmar la venta (Fuera de la transacción principal)
        try {
            List<DetalleDocumentoRequestDTO> detallesDocumento = ventaGuardada.getDetalles().stream()
                    .map(d -> DetalleDocumentoRequestDTO.builder()
                            .productoId(d.getProductoId())
                            .productoNombre(d.getProductoNombre())
                            .cantidad(d.getCantidad())
                            .precioUnitario(d.getPrecioUnitario())
                            .subtotal(d.getSubtotal())
                            .build())
                    .collect(Collectors.toList());

            EmitirDocumentoRequestDTO facturaRequest = EmitirDocumentoRequestDTO.builder()
                    .folioVenta(ventaGuardada.getFolio())
                    .ventaId(ventaGuardada.getId())
                    .usuarioId(ventaGuardada.getUsuarioId())
                    .sucursalId(ventaGuardada.getSucursalId())
                    .rutCliente(ventaGuardada.getClienteRut() != null && !ventaGuardada.getClienteRut().trim().isEmpty()
                            ? ventaGuardada.getClienteRut() : RUT_CLIENTE_GENERICO)
                    .tipoDocumento(ventaGuardada.getTipoDocumento())
                    .montoNeto((double) Math.round(ventaGuardada.getTotal().doubleValue() / 1.19)) // Neto real de la venta
                    .razonSocial(ventaGuardada.getRazonSocial())
                    .giro(ventaGuardada.getGiro())
                    .detalles(detallesDocumento)
                    .build();
            eventPublisher.publishEvent(new VentaRegistradaInternaEvent(this, facturaRequest));
            log.info("Evento VentaRegistradaInternaEvent publicado para folio: {}", ventaGuardada.getFolio());
        } catch (Exception e) {
            log.warn("No fue posible preparar el evento de documento tributario para la venta {}: {}", ventaGuardada.getFolio(), e.getMessage());
        }

        // 8. Descontar el stock en ms-inventario de forma síncrona (Feign) y fijar el estado final
        List<DetalleStockRequestDTO> detallesDescuento = ventaGuardada.getDetalles().stream()
                .map(d -> DetalleStockRequestDTO.builder()
                        .productoId(d.getProductoId())
                        .cantidad(d.getCantidad())
                        .build())
                .collect(Collectors.toList());

        DescontarStockRequestDTO descuentoRequest = DescontarStockRequestDTO.builder()
                .ventaId(ventaGuardada.getId())
                .folio(ventaGuardada.getFolio())
                .sucursalId(ventaGuardada.getSucursalId())
                .usuarioId(ventaGuardada.getUsuarioId())
                .detalles(detallesDescuento)
                .build();

        try {
            inventarioClient.descontarStock(descuentoRequest);
            ventaGuardada.setEstado(EstadoVenta.COMPLETADA);
            log.info("Stock descontado con éxito en ms-inventario. Venta ID: {} marcada como COMPLETADA.",
                    ventaGuardada.getId());
        } catch (Exception e) {
            // Cubre tanto el rechazo real de ms-inventario (stock agotado, FeignException) como la
            // caída de comunicación (fallback): en ambos casos la venta queda registrada, pero
            // marcada como RECHAZADA en lugar de fallar la petición completa.
            ventaGuardada.setEstado(EstadoVenta.RECHAZADA);
            log.warn("No fue posible descontar el stock en ms-inventario para la venta {}: {}. Venta marcada como RECHAZADA.",
                    ventaGuardada.getFolio(), e.getMessage());
        }
        ventaGuardada = ventaRepository.save(ventaGuardada);

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

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponseDTO> obtenerVentasPorUsuario(Long usuarioId) {
        log.info("Buscando historial de ventas para usuario ID: {}", usuarioId);
        return ventaRepository.findByUsuarioId(usuarioId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponseDTO obtenerVentaPorId(Long id) {
        log.info("Buscando venta con ID: {}", id);
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Venta no encontrada con ID: {}", id);
                    return new ResourceNotFoundException("La venta con el ID '" + id + "' no existe.");
                });
        return mapToResponse(venta);
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
                .sucursalId(venta.getSucursalId())
                .usuarioId(venta.getUsuarioId())
                .clienteNombre(venta.getClienteNombre())
                .clienteRut(venta.getClienteRut())
                .asistenteNombre(venta.getAsistenteNombre())
                .subtotal(venta.getSubtotal())
                .descuentoAplicado(venta.getDescuentoAplicado())
                .tipoDescuento(venta.getTipoDescuento())
                .codigoDescuento(venta.getCodigoDescuento())
                .total(venta.getTotal())
                .tipoDocumento(venta.getTipoDocumento())
                .razonSocial(venta.getRazonSocial())
                .giro(venta.getGiro())
                .detalles(detalleDTOs)
                .build();
    }
}
