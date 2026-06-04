package cl.bookpointchile.catalogo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResenaResponseDTO {
    private Long id;
    private Long usuarioId;
    private String usuarioNombre;
    private Integer calificacion;
    private String comentario;
}
