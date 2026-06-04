package cl.bookpointchile.proveedores.service;

import cl.bookpointchile.proveedores.dto.*;
import cl.bookpointchile.proveedores.exception.OrdenCompraInvalidaException;
import cl.bookpointchile.proveedores.exception.ProveedorNoEncontradoException;
import cl.bookpointchile.proveedores.exception.ResourceNotFoundException;
import cl.bookpointchile.proveedores.model.*;
import cl.bookpointchile.proveedores.repository.OrdenCompraRepository;
import cl.bookpointchile.proveedores.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProveedoresServiceImpl implements ProveedoresService {

    private final ProveedorRepository proveedorRepository;
    private final OrdenCompraRepository ordenCompraRepository;

    @Override
    @Transactional
    public ProveedorResponseDTO registrarProveedor(CrearProveedorRequestDTO request) {
        log.info("Registrando proveedor: '{}' (RUT: {})", request.getRazonSocial(), request.getRut());

        if (proveedorRepository.existsByRut(request.getRut())) {
            log.warn("Registro rechazado: Proveedor con RUT '{}' ya existe.", request.getRut());
            throw new OrdenCompraInvalidaException("El proveedor con RUT '" + request.getRut() + "' ya se encuentra registrado.");
        }

        Proveedor proveedor = Proveedor.builder()
                .rut(request.getRut())
                .razonSocial(request.getRazonSocial())
                .emailContacto(request.getEmailContacto())
                .telefono(request.getTelefono())
                .build();

        Proveedor saved = proveedorRepository.save(proveedor);
        log.info("Proveedor guardado exitosamente. ID Asignado: {}", saved.getId());
        return mapToProveedorResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorResponseDTO> obtenerTodosProveedores() {
        log.info("Buscando listado de todos los proveedores.");
        return proveedorRepository.findAll().stream()
                .map(this::mapToProveedorResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrdenCompraResponseDTO emitirOrdenCompra(OrdenCompraRequestDTO request) {
        log.info("Emitiendo nueva Orden de Compra (ODC) para el proveedor ID: {}", request.getProveedorId());

        Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
                .orElseThrow(() -> {
                    log.error("Emisión de ODC rechazada: Proveedor ID {} no existe.", request.getProveedorId());
                    return new ProveedorNoEncontradoException("El proveedor con ID " + request.getProveedorId() + " no existe.");
                });

        // Generar cabecera de la ODC
        OrdenCompra orden = OrdenCompra.builder()
                .proveedor(proveedor)
                .fechaEmision(LocalDateTime.now())
                .estado(EstadoOrden.PENDIENTE)
                .build();

        // Mapear y adjuntar los detalles
        for (DetalleOrdenRequestDTO item : request.getDetalles()) {
            if (item.getCantidadSolicitada() <= 0) {
                log.error("Emisión rechazada: Cantidad solicitada es cero o negativa para producto ID {}", item.getProductoId());
                throw new OrdenCompraInvalidaException("La cantidad solicitada en los detalles debe ser estrictamente mayor a cero.");
            }

            DetalleOrden detalle = DetalleOrden.builder()
                    .productoId(item.getProductoId())
                    .cantidadSolicitada(item.getCantidadSolicitada())
                    .costoUnitario(item.getCostoUnitario())
                    .build();

            orden.addDetalle(detalle);
        }

        OrdenCompra saved = ordenCompraRepository.save(orden);
        log.info("Orden de Compra emitida con éxito. ID ODC: {}, Proveedor: '{}', Total detalles: {}", 
                saved.getId(), saved.getProveedor().getRazonSocial(), saved.getDetalles().size());

        return mapToOdcResponse(saved);
    }

    @Override
    @Transactional
    public OrdenCompraResponseDTO registrarRecepcionMercaderia(Long id) {
        log.info("Solicitud de recepción de mercadería para Orden de Compra ID: {}", id);

        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Recepción fallida: La Orden de Compra ID {} no existe.", id);
                    return new ResourceNotFoundException("La Orden de Compra con ID " + id + " no fue encontrada.");
                });

        EstadoOrden estadoActual = orden.getEstado();

        if (estadoActual == EstadoOrden.RECIBIDA) {
            log.info("La Orden de Compra ID {} ya fue recibida previamente. Sin cambios.", id);
            return mapToOdcResponse(orden);
        }

        if (estadoActual == EstadoOrden.CANCELADA) {
            // Log de advertencia crítico para trazabilidad senior
            log.warn("RECEPCIÓN RECHAZADA: Intento de recepción de mercadería para la ODC ID {} que está CANCELADA.", id);
            throw new OrdenCompraInvalidaException("No se puede registrar la recepción de mercadería de una orden de compra CANCELADA.");
        }

        // Transicionar a RECIBIDA
        orden.setEstado(EstadoOrden.RECIBIDA);
        OrdenCompra saved = ordenCompraRepository.save(orden);
        log.info("Recepción de mercadería registrada con éxito para ODC ID: {}. Estado actualizado a RECIBIDA.", saved.getId());

        return mapToOdcResponse(saved);
    }

    // Helpers manuales de mapeo
    private ProveedorResponseDTO mapToProveedorResponse(Proveedor p) {
        return ProveedorResponseDTO.builder()
                .id(p.getId())
                .rut(p.getRut())
                .razonSocial(p.getRazonSocial())
                .emailContacto(p.getEmailContacto())
                .telefono(p.getTelefono())
                .build();
    }

    private OrdenCompraResponseDTO mapToOdcResponse(OrdenCompra o) {
        BigDecimal total = BigDecimal.ZERO;

        List<DetalleOrdenResponseDTO> detalleDTOs = o.getDetalles().stream()
                .map(d -> {
                    BigDecimal subtotal = d.getCostoUnitario().multiply(BigDecimal.valueOf(d.getCantidadSolicitada()));
                    return DetalleOrdenResponseDTO.builder()
                            .id(d.getId())
                            .productoId(d.getProductoId())
                            .cantidadSolicitada(d.getCantidadSolicitada())
                            .costoUnitario(d.getCostoUnitario())
                            .subtotal(subtotal)
                            .build();
                })
                .collect(Collectors.toList());

        for (DetalleOrdenResponseDTO d : detalleDTOs) {
            total = total.add(d.getSubtotal());
        }

        return OrdenCompraResponseDTO.builder()
                .id(o.getId())
                .proveedorId(o.getProveedor().getId())
                .proveedorRazonSocial(o.getProveedor().getRazonSocial())
                .fechaEmision(o.getFechaEmision())
                .estado(o.getEstado())
                .total(total)
                .detalles(detalleDTOs)
                .build();
    }
}
