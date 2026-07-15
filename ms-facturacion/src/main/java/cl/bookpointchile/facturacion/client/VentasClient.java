package cl.bookpointchile.facturacion.client;

import cl.bookpointchile.facturacion.dto.VentaResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ms-ventas", url = "${app.feign.ms-ventas.url:http://localhost:8081}")
public interface VentasClient {

    @GetMapping("/api/ventas/{folio}")
    VentaResponseDTO obtenerVentaPorFolio(@PathVariable("folio") String folio);
}
