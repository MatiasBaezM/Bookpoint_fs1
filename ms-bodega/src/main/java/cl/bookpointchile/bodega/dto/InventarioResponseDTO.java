package cl.bookpointchile.bodega.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventarioResponseDTO {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private String sku;
    private Integer cantidad;
    private Integer stockMinimo;
    private Long sucursalId;
    private String sucursalNombre;
    private boolean alertaReposicion;
}
