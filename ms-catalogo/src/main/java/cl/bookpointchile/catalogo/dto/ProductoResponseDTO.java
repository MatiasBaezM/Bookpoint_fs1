package cl.bookpointchile.catalogo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoResponseDTO {
    private Long id;
    private String titulo;
    private String autor;
    private String editorial;
    private BigDecimal precio;
    private String descripcion;
    private String categoria;
    private Double calificacionPromedio; // Promedio calculado dinámicamente en el Service
    private Integer totalResenas;        // Total de comentarios
    private List<ResenaResponseDTO> resenas;
}
