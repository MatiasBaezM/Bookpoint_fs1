package cl.bookpointchile.proveedores.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleOrdenResponseDTO {
    private Long id;
    private Long productoId;
    private Integer cantidadSolicitada;
    private BigDecimal costoUnitario;
    private BigDecimal subtotal;
}
