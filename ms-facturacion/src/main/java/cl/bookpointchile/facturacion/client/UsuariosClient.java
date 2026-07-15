package cl.bookpointchile.facturacion.client;

import cl.bookpointchile.facturacion.dto.UsuarioResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ms-usuarios", url = "${app.feign.ms-usuarios.url:http://localhost:8083}")
public interface UsuariosClient {

    @GetMapping("/api/usuarios/rut/{rut}")
    UsuarioResponseDTO obtenerUsuarioPorRut(@PathVariable("rut") String rut);
}
