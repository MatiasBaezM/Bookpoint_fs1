package cl.bookpointchile.inventario.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrasladoStockRequestDTO {

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;

    @NotNull(message = "La sucursal de origen es obligatoria")
    private Long sucursalOrigenId;

    @NotNull(message = "La sucursal de destino es obligatoria")
    private Long sucursalDestinoId;

    @NotNull(message = "La cantidad a trasladar es obligatoria")
    @Min(value = 1, message = "La cantidad mínima a trasladar es 1")
    private Integer cantidad;
}
