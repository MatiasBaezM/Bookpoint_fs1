package cl.bookpointchile.logistica.client;

import cl.bookpointchile.logistica.dto.SucursalResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "ms-sucursales", url = "${app.feign.ms-sucursales.url:http://localhost:8090}")
public interface SucursalesClient {

    @GetMapping("/api/sucursales")
    List<SucursalResponseDTO> obtenerTodas();
}
