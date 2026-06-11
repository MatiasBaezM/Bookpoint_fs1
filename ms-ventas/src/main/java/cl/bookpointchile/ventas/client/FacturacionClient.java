package cl.bookpointchile.ventas.client;

import cl.bookpointchile.ventas.dto.DocumentoResponseDTO;
import cl.bookpointchile.ventas.dto.EmitirDocumentoRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ms-facturacion", url = "${app.feign.ms-facturacion.url:http://localhost:8088}")
public interface FacturacionClient {

    @PostMapping("/api/facturacion/emitir")
    DocumentoResponseDTO emitirDocumento(@RequestBody EmitirDocumentoRequestDTO request);
}
