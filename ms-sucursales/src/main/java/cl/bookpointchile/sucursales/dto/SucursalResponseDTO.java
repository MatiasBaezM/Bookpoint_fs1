package cl.bookpointchile.sucursales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SucursalResponseDTO {
    private Long id;
    private String nombre;
    private String direccion;
    private String comuna;
    private String region;
    private String telefono;
    private String horarioAtencion;
    private String estadoOperativo;
}
