package cl.bookpointchile.bodega.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearOrdenPickingRequestDTO {

    @NotNull(message = "El ID de la venta es obligatorio")
    private Long ventaId;

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;

    @NotNull(message = "La cantidad a recolectar es obligatoria")
    @Min(value = 1, message = "La cantidad a recolectar debe ser al menos 1")
    private Integer cantidad;

    @NotBlank(message = "El operario asignado es obligatorio")
    private String operarioAsignado;
}
