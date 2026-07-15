package cl.bookpointchile.bodega.client;

import cl.bookpointchile.bodega.dto.VentaResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ms-ventas", url = "${app.feign.ms-ventas.url:http://localhost:8081}")
public interface VentasClient {

    @GetMapping("/api/ventas/id/{id}")
    VentaResponseDTO obtenerVentaPorId(@PathVariable("id") Long id);
}
