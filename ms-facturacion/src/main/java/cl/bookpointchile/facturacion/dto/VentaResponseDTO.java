package cl.bookpointchile.facturacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaResponseDTO {
    private Long id;
    private String folio;
    private Double total;
    private String clienteRut;
    private String tipoDocumento;
    private String estado;
}
