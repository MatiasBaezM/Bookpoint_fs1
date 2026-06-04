package cl.bookpointchile.proveedores.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleOrdenRequestDTO {

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;

    @NotNull(message = "La cantidad solicitada es obligatoria")
    @Min(value = 1, message = "La cantidad solicitada debe ser de al menos 1 unidad")
    private Integer cantidadSolicitada;

    @NotNull(message = "El costo unitario es obligatorio")
    @Min(value = 0, message = "El costo unitario no puede ser un valor negativo")
    private BigDecimal costoUnitario;
}
