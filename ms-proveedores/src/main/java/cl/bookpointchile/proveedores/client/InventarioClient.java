package cl.bookpointchile.proveedores.client;

import cl.bookpointchile.proveedores.dto.AjusteStockRequestDTO;
import cl.bookpointchile.proveedores.dto.InventarioResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ms-inventario", url = "${app.feign.ms-inventario.url:http://localhost:8082}")
public interface InventarioClient {

    @PutMapping("/api/inventario/ajuste")
    InventarioResponseDTO registrarAjuste(@RequestBody AjusteStockRequestDTO request);
}
