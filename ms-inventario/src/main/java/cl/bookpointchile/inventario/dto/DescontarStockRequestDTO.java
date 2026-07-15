package cl.bookpointchile.inventario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Petición síncrona de ms-ventas para descontar, en un solo paso, el stock de todas las
// líneas de una venta en la sucursal donde se realizó.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DescontarStockRequestDTO {

    private Long ventaId;
    private String folio;
    private Long usuarioId;

    @NotNull(message = "La sucursal es obligatoria")
    private Long sucursalId;

    @NotEmpty(message = "La venta debe incluir al menos un detalle de producto")
    @Valid
    private List<DetalleStockRequestDTO> detalles;
}
