package cl.bookpointchile.promociones.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromocionResponseDTO {
    private Long id;
    private String codigo;
    private Integer porcentajeDescuento;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String estado;
    private boolean vigente; // Campo dinámico calculado en base a fechas y estado
}
