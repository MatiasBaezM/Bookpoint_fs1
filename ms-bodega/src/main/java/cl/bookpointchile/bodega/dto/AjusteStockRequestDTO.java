package cl.bookpointchile.bodega.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AjusteStockRequestDTO {
    private Long productoId;
    private Long sucursalId;
    private Integer cantidadAjuste;
    private String motivo;
}
