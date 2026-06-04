package cl.bookpointchile.bodega.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "ordenes_picking",
    uniqueConstraints = @UniqueConstraint(name = "uk_picking_venta_id", columnNames = "venta_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenPicking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "venta_id", nullable = false, unique = true)
    private Long ventaId; // Una venta no puede ser armada en caja más de una vez

    @Column(name = "operario_asignado", nullable = false, length = 100)
    private String operarioAsignado; // Operario encargado de buscar los libros

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String estado = "PENDIENTE"; // PENDIENTE -> EN_PROCESO -> COMPLETADA

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion;
}
