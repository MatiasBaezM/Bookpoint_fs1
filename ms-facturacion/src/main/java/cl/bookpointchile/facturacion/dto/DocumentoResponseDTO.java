package cl.bookpointchile.facturacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoResponseDTO {
    private Long id;
    private String folioVenta;
    private Long ventaId;
    private Long usuarioId;
    private Long sucursalId;
    private String rutCliente;
    private String razonSocial;
    private String giro;
    private String tipoDocumento;
    private Double montoNeto;
    private Double montoIva;
    private Double montoTotal;
    private LocalDateTime fechaEmision;
    private List<DetalleDocumentoResponseDTO> detalles;
}
