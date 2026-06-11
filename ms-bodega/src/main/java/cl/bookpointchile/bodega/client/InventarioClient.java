package cl.bookpointchile.bodega.client;

import cl.bookpointchile.bodega.dto.AjusteStockRequestDTO;
import cl.bookpointchile.bodega.dto.InventarioResponseDTO;
import cl.bookpointchile.bodega.dto.StockResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ms-inventario", url = "${app.feign.ms-inventario.url:http://localhost:8082}", fallback = InventarioClientFallback.class)
public interface InventarioClient {

    @GetMapping("/api/inventario/check-stock")
    StockResponseDTO checkStock(
            @RequestParam("productoId") Long productoId,
            @RequestParam("cantidad") Integer cantidad
    );

    @PutMapping("/api/inventario/ajuste")
    InventarioResponseDTO registrarAjuste(@RequestBody AjusteStockRequestDTO request);
}
