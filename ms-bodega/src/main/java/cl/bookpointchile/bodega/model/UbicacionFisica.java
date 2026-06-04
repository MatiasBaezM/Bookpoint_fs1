package cl.bookpointchile.bodega.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "ubicaciones_fisicas",
    uniqueConstraints = @UniqueConstraint(name = "uk_ubicacion_codigo_barras", columnNames = "codigo_barras")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UbicacionFisica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String pasillo; // Ej: "Pasillo A"

    @Column(nullable = false, length = 50)
    private String estante; // Ej: "Estante 1"

    @Column(nullable = false, length = 50)
    private String nivel; // Ej: "Nivel 2"

    @Column(name = "codigo_barras", nullable = false, unique = true, length = 100)
    private String codigoBarras; // Ej: "P-A-E1-N2"
}
