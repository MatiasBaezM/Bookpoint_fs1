package cl.bookpointchile.sucursales.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SucursalRequestDTO {

    @NotBlank(message = "El nombre de la sucursal es obligatorio")
    private String nombre;

    @NotBlank(message = "La dirección de la sucursal es obligatoria")
    private String direccion;

    @NotBlank(message = "La comuna es obligatoria")
    private String comuna;

    private String region; // Opcional (ej: "Región del Biobío")

    private String telefono; // Opcional (ej: "+56 41 234 5678")

    private String horarioAtencion; // Opcional (ej: "Lunes a Viernes 09:00 a 18:30")

    private String estadoOperativo; // Opcional (por defecto 'ACTIVO')
}
