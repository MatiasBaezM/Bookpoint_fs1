package cl.bookpointchile.bodega.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenPickingResponseDTO {
    private Long id;
    private Long ventaId;
    private String operarioAsignado;
    private String estado;
    private LocalDateTime fechaAsignacion;
}
