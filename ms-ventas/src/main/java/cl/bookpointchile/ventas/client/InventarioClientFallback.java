package cl.bookpointchile.ventas.client;

import cl.bookpointchile.ventas.dto.DescontarStockRequestDTO;
import cl.bookpointchile.ventas.dto.StockResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class InventarioClientFallback implements InventarioClient {

    @Override
    public StockResponseDTO checkStock(Long sucursalId, Long productoId, Integer cantidad) {
        // Simulación: Si el ID del producto es 999, simulamos que no hay stock suficiente
        if (productoId == 999L) {
            return StockResponseDTO.builder()
                    .productoId(productoId)
                    .sucursalId(sucursalId)
                    .disponible(false)
                    .stockActual(0)
                    .build();
        }

        // Simulación por defecto: Stock disponible
        return StockResponseDTO.builder()
                .productoId(productoId)
                .sucursalId(sucursalId)
                .disponible(true)
                .stockActual(cantidad + 15)
                .build();
    }

    @Override
    public void descontarStock(DescontarStockRequestDTO request) {
        // A diferencia de checkStock, aquí NO se debe asumir éxito: si ms-inventario no responde,
        // no hay forma de confirmar que el stock realmente se descontó. Se falla "cerrado" para
        // que la venta quede marcada como RECHAZADA en vez de arriesgar una descoordinación de stock.
        throw new IllegalStateException("ms-inventario no disponible; no fue posible confirmar el descuento de stock para la venta " +
                request.getFolio() + ".");
    }
}
