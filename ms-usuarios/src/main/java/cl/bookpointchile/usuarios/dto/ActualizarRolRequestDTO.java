package cl.bookpointchile.usuarios.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarRolRequestDTO {

    @NotNull(message = "El ID del nuevo rol es obligatorio")
    private Long rolId;
}
