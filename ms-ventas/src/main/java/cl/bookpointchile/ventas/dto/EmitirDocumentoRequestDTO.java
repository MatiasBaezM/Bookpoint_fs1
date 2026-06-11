package cl.bookpointchile.ventas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmitirDocumentoRequestDTO {
    private String folioVenta;
    private String rutCliente;
    private String razonSocial;
    private String giro;
    private String tipoDocumento;
    private Double montoNeto;
}
