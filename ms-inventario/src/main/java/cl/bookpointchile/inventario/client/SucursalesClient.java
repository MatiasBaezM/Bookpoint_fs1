package cl.bookpointchile.inventario.client;

import cl.bookpointchile.inventario.dto.SucursalMaestraResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ms-sucursales", url = "${app.feign.ms-sucursales.url:http://localhost:8090}")
public interface SucursalesClient {

    @GetMapping("/api/sucursales/{id}")
    SucursalMaestraResponseDTO obtenerPorId(@PathVariable("id") Long id);
}
