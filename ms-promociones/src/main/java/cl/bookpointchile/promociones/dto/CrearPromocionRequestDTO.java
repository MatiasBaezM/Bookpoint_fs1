package cl.bookpointchile.promociones.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearPromocionRequestDTO {

    @NotBlank(message = "El código promocional es obligatorio")
    private String codigo;

    @NotNull(message = "El porcentaje de descuento es obligatorio")
    @Min(value = 1, message = "El descuento mínimo es 1%")
    @Max(value = 100, message = "El descuento máximo es 100%")
    private Integer porcentajeDescuento;

    @NotNull(message = "La fecha de inicio de la promoción es obligatoria")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de término de la promoción es obligatoria")
    private LocalDate fechaFin;

    private String estado; // Opcional (si se omite, se asigna por defecto 'ACTIVO')
}
