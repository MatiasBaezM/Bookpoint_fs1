package cl.bookpointchile.inventario.service;

import cl.bookpointchile.inventario.client.SucursalesClient;
import cl.bookpointchile.inventario.dto.*;
import cl.bookpointchile.inventario.exception.ResourceNotFoundException;
import cl.bookpointchile.inventario.exception.StockInsuficienteException;
import cl.bookpointchile.inventario.exception.SucursalNoEncontradaException;
import cl.bookpointchile.inventario.model.Inventario;
import cl.bookpointchile.inventario.repository.InventarioRepository;
import feign.FeignException;
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
    private final SucursalesClient sucursalesClient;

    @Override
    @Transactional
    public InventarioResponseDTO registrarAjusteFisico(AjusteStockRequestDTO request) {
        log.info("Iniciando ajuste físico. Sucursal ID: {}, Producto ID: {}, Cantidad Ajuste: {}, Motivo: '{}'",
                request.getSucursalId(), request.getProductoId(), request.getCantidadAjuste(), request.getMotivo());

        SucursalMaestraResponseDTO sucursalInfo = validarSucursalActivaEnMaestro(request.getSucursalId());

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
                    request.getProductoId(), sucursalInfo.getNombre());
            
            inventario = Inventario.builder()
                    .productoId(request.getProductoId())
                    .productoNombre("Producto Genérico ID " + request.getProductoId())
                    .sku("SKU-" + request.getProductoId() + "-" + sucursalInfo.getId())
                    .cantidad(request.getCantidadAjuste())
                    .stockMinimo(5) // Stock mínimo por defecto
                    .sucursalId(sucursalInfo.getId())
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

        SucursalMaestraResponseDTO origen = validarSucursalActivaEnMaestro(request.getSucursalOrigenId());
        SucursalMaestraResponseDTO destino = validarSucursalActivaEnMaestro(request.getSucursalDestinoId());

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
                    .sucursalId(destino.getId())
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
        
        // Verificar que la sucursal exista en el maestro
        validarSucursalActivaEnMaestro(sucursalId);

        Inventario inventario = inventarioRepository.findByProductoIdAndSucursalId(productoId, sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException("El producto con ID " + productoId + 
                        " no tiene stock registrado en la sucursal ID " + sucursalId));

        return mapToResponse(inventario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventarioResponseDTO> obtenerStockPorSucursal(Long sucursalId) {
        log.info("Obteniendo inventario completo para Sucursal ID: {}", sucursalId);
        
        // Verificar que la sucursal exista en el maestro
        validarSucursalActivaEnMaestro(sucursalId);

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
    public StockResponseDTO verificarDisponibilidad(Long sucursalId, Long productoId, Integer cantidad) {
        log.info("Verificando disponibilidad de stock en Sucursal ID: {} para producto ID: {}, cantidad: {}",
                sucursalId, productoId, cantidad);

        int stockActual = inventarioRepository.findByProductoIdAndSucursalId(productoId, sucursalId)
                .map(Inventario::getCantidad)
                .orElse(0);

        boolean disponible = stockActual >= cantidad;
        log.info("Resultado stock para producto ID {} en sucursal ID {}: Disponible = {}, Cantidad Solicitada = {}, ¿Alcanza? = {}",
                productoId, sucursalId, stockActual, cantidad, disponible);

        return StockResponseDTO.builder()
                .productoId(productoId)
                .sucursalId(sucursalId)
                .disponible(disponible)
                .stockActual(stockActual)
                .build();
    }

    @Override
    @Transactional
    public void descontarStockVenta(DescontarStockRequestDTO request) {
        Long sucursalId = request.getSucursalId();
        log.info("Descontando stock de la Sucursal ID: {} para Venta ID: {}, Folio: {}",
                sucursalId, request.getVentaId(), request.getFolio());

        if (sucursalId == null) {
            throw new SucursalNoEncontradaException("La venta '" + request.getFolio() +
                    "' no indica la sucursal en la que se realizó; no es posible descontar el stock.");
        }

        SucursalMaestraResponseDTO sucursal = validarSucursalActivaEnMaestro(sucursalId);

        // Primero se validan todos los detalles contra el stock de ESA sucursal, para no
        // descontar nada si alguna línea no alcanza.
        for (DetalleStockRequestDTO detalle : request.getDetalles()) {
            Inventario inv = inventarioRepository
                    .findByProductoIdAndSucursalId(detalle.getProductoId(), sucursalId)
                    .orElseThrow(() -> new StockInsuficienteException("El producto ID " + detalle.getProductoId() +
                            " no tiene stock registrado en la sucursal '" + sucursal.getNombre() + "'."));

            if (inv.getCantidad() < detalle.getCantidad()) {
                log.warn("Stock insuficiente en sucursal '{}' para Producto ID: {}. Solicitado: {}, Disponible: {}",
                        sucursal.getNombre(), detalle.getProductoId(), detalle.getCantidad(), inv.getCantidad());
                throw new StockInsuficienteException("Stock insuficiente para el producto ID " + detalle.getProductoId() +
                        " en la sucursal '" + sucursal.getNombre() + "'. Disponible: " + inv.getCantidad() +
                        ", Solicitado: " + detalle.getCantidad());
            }
        }

        for (DetalleStockRequestDTO detalle : request.getDetalles()) {
            Inventario inv = inventarioRepository
                    .findByProductoIdAndSucursalId(detalle.getProductoId(), sucursalId)
                    .orElseThrow(() -> new StockInsuficienteException("El producto ID " + detalle.getProductoId() +
                            " no tiene stock registrado en la sucursal '" + sucursal.getNombre() + "'."));

            inv.setCantidad(inv.getCantidad() - detalle.getCantidad());
            inventarioRepository.save(inv);
            log.info("Stock descontado. Producto ID: {}, Sucursal: '{}', Descontado: {}, Stock resultante: {}",
                    detalle.getProductoId(), sucursal.getNombre(), detalle.getCantidad(), inv.getCantidad());
        }

        log.info("Descuento de stock completado para Venta ID: {} en la sucursal '{}'.",
                request.getVentaId(), sucursal.getNombre());
    }

    // Valida la sucursal local contra el maestro de sucursales (ms-sucursales) y retorna su información
    private SucursalMaestraResponseDTO validarSucursalActivaEnMaestro(Long sucursalId) {
        try {
            SucursalMaestraResponseDTO maestra = sucursalesClient.obtenerPorId(sucursalId);
            if (maestra == null) {
                throw new SucursalNoEncontradaException("La sucursal con ID " + sucursalId + " no existe en el maestro de sucursales.");
            }
            if (!"ACTIVO".equalsIgnoreCase(maestra.getEstadoOperativo())) {
                log.error("Sucursal '{}' (ID {}) no está activa en el maestro de sucursales. Estado: {}",
                        maestra.getNombre(), sucursalId, maestra.getEstadoOperativo());
                throw new SucursalNoEncontradaException("La sucursal '" + maestra.getNombre() +
                        "' no se encuentra operativa en el maestro de sucursales.");
            }
            return maestra;
        } catch (SucursalNoEncontradaException e) {
            throw e;
        } catch (FeignException.NotFound e) {
            log.error("Sucursal con ID {} no existe en el maestro de sucursales.", sucursalId);
            throw new SucursalNoEncontradaException("La sucursal con ID " + sucursalId + " no existe en el maestro de sucursales.");
        } catch (Exception e) {
            log.warn("No fue posible validar la sucursal ID {} contra ms-sucursales: {}", sucursalId, e.getMessage());
            // En caso de caída de comunicación con el maestro, toleramos fallos construyendo un DTO temporal de respaldo
            return SucursalMaestraResponseDTO.builder()
                    .id(sucursalId)
                    .nombre("Sucursal Temp (ID " + sucursalId + ")")
                    .estadoOperativo("ACTIVO")
                    .build();
        }
    }

    // Helper Mapper manual
    private InventarioResponseDTO mapToResponse(Inventario i) {
        String sucursalNombre = "Sucursal Desconocida (ID " + i.getSucursalId() + ")";
        try {
            SucursalMaestraResponseDTO sucursal = sucursalesClient.obtenerPorId(i.getSucursalId());
            if (sucursal != null) {
                sucursalNombre = sucursal.getNombre();
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener el nombre de la sucursal ID {} desde ms-sucursales: {}", i.getSucursalId(), e.getMessage());
        }
        return InventarioResponseDTO.builder()
                .id(i.getId())
                .productoId(i.getProductoId())
                .productoNombre(i.getProductoNombre())
                .sku(i.getSku())
                .cantidad(i.getCantidad())
                .stockMinimo(i.getStockMinimo())
                .sucursalId(i.getSucursalId())
                .sucursalNombre(sucursalNombre)
                .alertaReposicion(i.getCantidad() <= i.getStockMinimo())
                .build();
    }
}
