package cl.bookpointchile.logistica.dto;

import cl.bookpointchile.logistica.model.EstadoEnvio;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarEstadoRequestDTO {

    @NotNull(message = "El nuevo estado de envío es obligatorio")
    private EstadoEnvio estado;
}
