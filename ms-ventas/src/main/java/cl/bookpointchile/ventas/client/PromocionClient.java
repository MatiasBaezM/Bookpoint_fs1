package cl.bookpointchile.ventas.client;

import cl.bookpointchile.ventas.dto.PromocionResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ms-promociones", url = "${app.feign.ms-promociones.url:http://localhost:8087}")
public interface PromocionClient {

    @GetMapping("/api/promociones/validar/{codigo}")
    PromocionResponseDTO validarPromocion(@PathVariable("codigo") String codigo);
}
