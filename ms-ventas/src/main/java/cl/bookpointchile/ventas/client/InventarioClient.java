package cl.bookpointchile.ventas.client;

import cl.bookpointchile.ventas.dto.DescontarStockRequestDTO;
import cl.bookpointchile.ventas.dto.StockResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ms-inventario", url = "${app.feign.ms-inventario.url:http://localhost:8082}", fallback = InventarioClientFallback.class)
public interface InventarioClient {

    @GetMapping("/api/inventario/check-stock")
    StockResponseDTO checkStock(
            @RequestParam("sucursalId") Long sucursalId,
            @RequestParam("productoId") Long productoId,
            @RequestParam("cantidad") Integer cantidad
    );

    // Llamada síncrona que reemplaza el antiguo mensaje asíncrono a RabbitMQ: descuenta,
    // en un solo paso, el stock de todas las líneas de la venta en su sucursal.
    @PostMapping("/api/inventario/descuento")
    void descontarStock(@RequestBody DescontarStockRequestDTO request);
}
