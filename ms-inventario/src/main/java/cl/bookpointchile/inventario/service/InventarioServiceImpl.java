package cl.bookpointchile.inventario.service;

import cl.bookpointchile.inventario.dto.*;
import cl.bookpointchile.inventario.exception.ResourceNotFoundException;
import cl.bookpointchile.inventario.exception.StockInsuficienteException;
import cl.bookpointchile.inventario.exception.SucursalNoEncontradaException;
import cl.bookpointchile.inventario.model.Inventario;
import cl.bookpointchile.inventario.model.Sucursal;
import cl.bookpointchile.inventario.repository.InventarioRepository;
import cl.bookpointchile.inventario.repository.SucursalRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventarioServiceImpl implements InventarioService {

    private final InventarioRepository inventarioRepository;
    private final SucursalRepository sucursalRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public InventarioResponseDTO registrarAjusteFisico(AjusteStockRequestDTO request) {
        log.info("Iniciando ajuste físico. Sucursal ID: {}, Producto ID: {}, Cantidad Ajuste: {}, Motivo: '{}'",
                request.getSucursalId(), request.getProductoId(), request.getCantidadAjuste(), request.getMotivo());

        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
                .orElseThrow(() -> {
                    log.error("Ajuste fallido: Sucursal con ID {} no existe.", request.getSucursalId());
                    return new SucursalNoEncontradaException("La sucursal con ID " + request.getSucursalId() + " no fue encontrada.");
                });

        Inventario inventario = inventarioRepository
                .findByProductoIdAndSucursalId(request.getProductoId(), request.getSucursalId())
                .orElse(null);

        if (inventario == null) {
            // Si el registro de inventario no existe y el ajuste es negativo, no se puede realizar
            if (request.getCantidadAjuste() < 0) {
                log.error("Ajuste fallido: Intento de restar stock a un producto inexistente.");
                throw new StockInsuficienteException("No se puede realizar un ajuste negativo en un producto sin inventario previo.");
            }

            // Crear nuevo registro de inventario (por defecto, asignamos SKU y stock mínimo genéricos)
            log.info("Producto ID {} no registrado en la sucursal '{}'. Creando nuevo registro de inventario.", 
                    request.getProductoId(), sucursal.getNombre());
            
            inventario = Inventario.builder()
                    .productoId(request.getProductoId())
                    .productoNombre("Producto Genérico ID " + request.getProductoId())
                    .sku("SKU-" + request.getProductoId() + "-" + sucursal.getId())
                    .cantidad(request.getCantidadAjuste())
                    .stockMinimo(5) // Stock mínimo por defecto
                    .sucursal(sucursal)
                    .build();
        } else {
            // Si ya existe, validamos que no quede en negativo
            int nuevaCantidad = inventario.getCantidad() + request.getCantidadAjuste();
            if (nuevaCantidad < 0) {
                log.error("Ajuste fallido: El ajuste de {} en stock actual {} dejaría el inventario en negativo ({}).", 
                        request.getCantidadAjuste(), inventario.getCantidad(), nuevaCantidad);
                throw new StockInsuficienteException("El ajuste físico no puede ser procesado porque dejaría el stock en negativo. " +
                        "Stock actual: " + inventario.getCantidad() + ", Ajuste solicitado: " + request.getCantidadAjuste());
            }
            inventario.setCantidad(nuevaCantidad);
        }

        Inventario saved = inventarioRepository.save(inventario);
        log.info("Ajuste físico de inventario completado con éxito. Producto: {}, Cantidad Resultante: {}", 
                saved.getProductoNombre(), saved.getCantidad());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public InventarioResponseDTO trasladarStock(TrasladoStockRequestDTO request) {
        log.info("Iniciando traslado de stock. Origen ID: {}, Destino ID: {}, Producto ID: {}, Cantidad: {}",
                request.getSucursalOrigenId(), request.getSucursalDestinoId(), request.getProductoId(), request.getCantidad());

        if (request.getSucursalOrigenId().equals(request.getSucursalDestinoId())) {
            log.error("Traslado fallido: Origen y destino son iguales.");
            throw new StockInsuficienteException("La sucursal de origen y destino del traslado no pueden ser la misma.");
        }

        Sucursal origen = sucursalRepository.findById(request.getSucursalOrigenId())
                .orElseThrow(() -> new SucursalNoEncontradaException("La sucursal de origen con ID " + request.getSucursalOrigenId() + " no existe."));

        Sucursal destino = sucursalRepository.findById(request.getSucursalDestinoId())
                .orElseThrow(() -> new SucursalNoEncontradaException("La sucursal de destino con ID " + request.getSucursalDestinoId() + " no existe."));

        Inventario inventarioOrigen = inventarioRepository
                .findByProductoIdAndSucursalId(request.getProductoId(), request.getSucursalOrigenId())
                .orElseThrow(() -> {
                    log.error("Traslado fallido: Producto ID {} no tiene stock registrado en origen.", request.getProductoId());
                    return new StockInsuficienteException("El producto no tiene registro de stock en la sucursal de origen.");
                });

        if (inventarioOrigen.getCantidad() < request.getCantidad()) {
            log.error("Traslado fallido: Stock insuficiente en origen. Disponible: {}, Solicitado: {}", 
                    inventarioOrigen.getCantidad(), request.getCantidad());
            throw new StockInsuficienteException("Stock insuficiente en la sucursal de origen '" + origen.getNombre() + 
                    "'. Disponible: " + inventarioOrigen.getCantidad() + ", Solicitado a trasladar: " + request.getCantidad());
        }

        // Descontar del origen
        inventarioOrigen.setCantidad(inventarioOrigen.getCantidad() - request.getCantidad());
        inventarioRepository.save(inventarioOrigen);
        log.info("Descontado stock de origen. Nuevo stock en origen: {}", inventarioOrigen.getCantidad());

        // Aumentar en el destino (crear si no existe en destino)
        Inventario inventarioDestino = inventarioRepository
                .findByProductoIdAndSucursalId(request.getProductoId(), request.getSucursalDestinoId())
                .orElse(null);

        if (inventarioDestino == null) {
            log.info("Creando nuevo registro de inventario en destino '{}' para producto ID {}", 
                    destino.getNombre(), request.getProductoId());
            inventarioDestino = Inventario.builder()
                    .productoId(request.getProductoId())
                    .productoNombre(inventarioOrigen.getProductoNombre())
                    .sku(inventarioOrigen.getSku().split("-")[0] + "-" + destino.getId())
                    .cantidad(request.getCantidad())
                    .stockMinimo(inventarioOrigen.getStockMinimo())
                    .sucursal(destino)
                    .build();
        } else {
            inventarioDestino.setCantidad(inventarioDestino.getCantidad() + request.getCantidad());
        }

        Inventario savedDestino = inventarioRepository.save(inventarioDestino);
        log.info("Traslado completado con éxito. Stock trasladado a destino '{}'. Nuevo stock en destino: {}", 
                destino.getNombre(), savedDestino.getCantidad());

        return mapToResponse(savedDestino);
    }

    @Override
    @Transactional(readOnly = true)
    public InventarioResponseDTO obtenerStock(Long sucursalId, Long productoId) {
        log.info("Buscando stock para Sucursal ID: {} y Producto ID: {}", sucursalId, productoId);
        
        // Verificar que la sucursal exista
        if (!sucursalRepository.existsById(sucursalId)) {
            throw new SucursalNoEncontradaException("La sucursal con ID " + sucursalId + " no existe.");
        }

        Inventario inventario = inventarioRepository.findByProductoIdAndSucursalId(productoId, sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException("El producto con ID " + productoId + 
                        " no tiene stock registrado en la sucursal ID " + sucursalId));

        return mapToResponse(inventario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventarioResponseDTO> obtenerStockPorSucursal(Long sucursalId) {
        log.info("Obteniendo inventario completo para Sucursal ID: {}", sucursalId);
        
        if (!sucursalRepository.existsById(sucursalId)) {
            throw new SucursalNoEncontradaException("La sucursal con ID " + sucursalId + " no existe.");
        }

        return inventarioRepository.findBySucursalId(sucursalId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventarioResponseDTO> obtenerAlertasReposicion() {
        log.info("Obteniendo listado de alertas de stock bajo nivel mínimo.");
        return inventarioRepository.findAlertasStock().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StockResponseDTO verificarDisponibilidad(Long productoId, Integer cantidad) {
        log.info("Verificando disponibilidad de stock global/centralizado para producto ID: {}, cantidad: {}", productoId, cantidad);
        
        // Simular consulta: buscamos en todas las sucursales y sumamos el stock
        // (En un caso real de venta presencial, se consultaría a la sucursal específica de la caja,
        // y en online se consultaría a la Bodega Central. Aquí sumamos el stock total para simplificar e integrar con ms-ventas).
        List<Inventario> stocks = inventarioRepository.findAll().stream()
                .filter(i -> i.getProductoId().equals(productoId))
                .collect(Collectors.toList());

        int stockActualTotal = stocks.stream()
                .mapToInt(Inventario::getCantidad)
                .sum();

        boolean disponible = stockActualTotal >= cantidad;
        log.info("Resultado stock para producto ID {}: Total disponible = {}, Cantidad Solicitada = {}, ¿Disponible? = {}", 
                productoId, stockActualTotal, cantidad, disponible);

        return StockResponseDTO.builder()
                .productoId(productoId)
                .disponible(disponible)
                .stockActual(stockActualTotal)
                .build();
    }

    @Override
    @Transactional
    public void procesarVentaCreada(cl.bookpointchile.inventario.event.VentaCreadaEvent event) {
        log.info("Procesando VentaCreadaEvent para Venta ID: {}, Folio: {}", event.getVentaId(), event.getFolio());
        
        try {
            // Verificar stock para todos los detalles primero
            for (cl.bookpointchile.inventario.event.DetalleVentaEvent detalle : event.getDetalles()) {
                StockResponseDTO stock = verificarDisponibilidad(detalle.getProductoId(), detalle.getCantidad());
                if (!stock.isDisponible()) {
                    log.warn("Stock insuficiente para Producto ID: {}. Solicitado: {}, Disponible: {}", detalle.getProductoId(), detalle.getCantidad(), stock.getStockActual());
                    throw new RuntimeException("Stock insuficiente para Producto ID: " + detalle.getProductoId() + ". Disponible: " + stock.getStockActual());
                }
            }

            // Si todos tienen stock, proceder a descontar de forma global (empezando por la primera sucursal con stock)
            for (cl.bookpointchile.inventario.event.DetalleVentaEvent detalle : event.getDetalles()) {
                List<Inventario> stocks = inventarioRepository.findAll().stream()
                        .filter(i -> i.getProductoId().equals(detalle.getProductoId()) && i.getCantidad() > 0)
                        .collect(Collectors.toList());
                
                int cantidadPorDescontar = detalle.getCantidad();
                for (Inventario inv : stocks) {
                    if (cantidadPorDescontar <= 0) break;
                    
                    int descontar = Math.min(inv.getCantidad(), cantidadPorDescontar);
                    inv.setCantidad(inv.getCantidad() - descontar);
                    inventarioRepository.save(inv);
                    cantidadPorDescontar -= descontar;
                }
            }

            // Publicar StockReservadoEvent
            cl.bookpointchile.inventario.event.StockReservadoEvent reservadoEvent = cl.bookpointchile.inventario.event.StockReservadoEvent.builder()
                    .ventaId(event.getVentaId())
                    .folio(event.getFolio())
                    .build();
            rabbitTemplate.convertAndSend(cl.bookpointchile.inventario.config.RabbitMQConfig.EXCHANGE_VENTAS, cl.bookpointchile.inventario.config.RabbitMQConfig.ROUTING_KEY_STOCK_RESERVADO, reservadoEvent);
            log.info("StockReservadoEvent emitido exitosamente para Venta ID: {}", event.getVentaId());

        } catch (Exception e) {
            // Publicar StockRechazadoEvent
            cl.bookpointchile.inventario.event.StockRechazadoEvent rechazadoEvent = cl.bookpointchile.inventario.event.StockRechazadoEvent.builder()
                    .ventaId(event.getVentaId())
                    .folio(event.getFolio())
                    .motivo(e.getMessage())
                    .build();
            rabbitTemplate.convertAndSend(cl.bookpointchile.inventario.config.RabbitMQConfig.EXCHANGE_VENTAS, cl.bookpointchile.inventario.config.RabbitMQConfig.ROUTING_KEY_STOCK_RECHAZADO, rechazadoEvent);
            log.info("StockRechazadoEvent emitido por falta de stock u error para Venta ID: {}", event.getVentaId());
        }
    }

    // Helper Mapper manual
    private InventarioResponseDTO mapToResponse(Inventario i) {
        return InventarioResponseDTO.builder()
                .id(i.getId())
                .productoId(i.getProductoId())
                .productoNombre(i.getProductoNombre())
                .sku(i.getSku())
                .cantidad(i.getCantidad())
                .stockMinimo(i.getStockMinimo())
                .sucursalId(i.getSucursal().getId())
                .sucursalNombre(i.getSucursal().getNombre())
                .alertaReposicion(i.getCantidad() <= i.getStockMinimo())
                .build();
    }
}
