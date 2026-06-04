package cl.bookpointchile.bodega.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UbicacionRequestDTO {

    @NotBlank(message = "El pasillo es obligatorio")
    private String pasillo;

    @NotBlank(message = "El estante es obligatorio")
    private String estante;

    @NotBlank(message = "El nivel es obligatorio")
    private String nivel;

    @NotBlank(message = "El código de barras es obligatorio")
    private String codigoBarras;
}
