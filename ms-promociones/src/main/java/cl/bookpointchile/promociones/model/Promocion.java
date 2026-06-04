package cl.bookpointchile.promociones.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "promociones",
    uniqueConstraints = @UniqueConstraint(name = "uk_promocion_codigo", columnNames = "codigo")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo; // e.g. "CONVENIO_ESTUDIANTIL", "DESCUENTO10", "VERANO2025"

    @Column(name = "porcentaje_descuento", nullable = false)
    private Integer porcentajeDescuento; // Valor del 1 al 100

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "ACTIVO"; // ACTIVO, INACTIVO
}
