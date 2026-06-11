package cl.bookpointchile.ventas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoResponseDTO {
    private Long id;
    private String folioVenta;
    private String rutCliente;
    private String razonSocial;
    private String giro;
    private String tipoDocumento;
    private Double montoNeto;
    private Double montoIva;
    private Double montoTotal;
    private LocalDateTime fechaEmision;
}
