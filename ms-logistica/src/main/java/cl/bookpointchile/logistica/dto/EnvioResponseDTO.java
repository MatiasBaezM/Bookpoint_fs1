package cl.bookpointchile.logistica.dto;

import cl.bookpointchile.logistica.model.EstadoEnvio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvioResponseDTO {
    private Long id;
    private Long ventaId;
    private String direccionDestino;
    private EstadoEnvio estado;
    private LocalDate fechaEstimada;
    private Long rutaId;
    private String origen;
    private String destino;
    private String transportista;
}
