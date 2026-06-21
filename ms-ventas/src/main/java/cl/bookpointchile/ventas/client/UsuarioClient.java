package cl.bookpointchile.ventas.client;

import cl.bookpointchile.ventas.dto.UsuarioResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ms-usuarios", url = "${app.feign.ms-usuarios.url:http://localhost:8089}")
public interface UsuarioClient {

    @GetMapping("/api/usuarios/{id}")
    UsuarioResponseDTO obtenerUsuarioPorId(@PathVariable("id") Long id);
}
