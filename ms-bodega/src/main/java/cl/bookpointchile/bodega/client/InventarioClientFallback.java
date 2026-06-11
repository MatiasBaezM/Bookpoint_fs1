package cl.bookpointchile.bodega.client;

import cl.bookpointchile.bodega.dto.AjusteStockRequestDTO;
import cl.bookpointchile.bodega.dto.InventarioResponseDTO;
import cl.bookpointchile.bodega.dto.StockResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InventarioClientFallback implements InventarioClient {

    @Override
    public StockResponseDTO checkStock(Long productoId, Integer cantidad) {
        log.warn("ms-inventario no disponible. Aplicando fallback de verificación de stock para producto ID {}.", productoId);
        // Simulación por defecto: Stock disponible (no bloquear la operación de bodega ante una caída transitoria)
        return StockResponseDTO.builder()
                .productoId(productoId)
                .disponible(true)
                .stockActual(cantidad)
                .build();
    }

    @Override
    public InventarioResponseDTO registrarAjuste(AjusteStockRequestDTO request) {
        log.warn("ms-inventario no disponible. No fue posible registrar el ajuste de stock para producto ID {}.", request.getProductoId());
        return InventarioResponseDTO.builder()
                .productoId(request.getProductoId())
                .sucursalId(request.getSucursalId())
                .cantidad(request.getCantidadAjuste())
                .build();
    }
}
