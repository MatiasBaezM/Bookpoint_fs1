package cl.bookpointchile.ventas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleStockRequestDTO {
    private Long productoId;
    private Integer cantidad;
}
