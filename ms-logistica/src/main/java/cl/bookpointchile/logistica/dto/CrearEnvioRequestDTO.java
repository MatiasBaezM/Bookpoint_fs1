package cl.bookpointchile.logistica.dto;

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
public class CrearEnvioRequestDTO {

    @NotNull(message = "El ID de la venta es obligatorio")
    private Long ventaId;

    @NotBlank(message = "La dirección de destino es obligatoria")
    private String direccionDestino;

    private Long rutaId; // Opcional (si es nulo, el servicio asociará una ruta óptima por comuna)
}
