package cl.bookpointchile.sucursales.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "sucursales",
    uniqueConstraints = @UniqueConstraint(name = "uk_sucursal_nombre", columnNames = "nombre")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sucursal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre; // Ej: "Bodega Central Concepción" (Debe ser único)

    @Column(nullable = false, length = 150)
    private String direccion; // Ej: "Av. Las Golondrinas 1234"

    @Column(nullable = false, length = 100)
    private String comuna; // Ej: "Hualpén"

    @Column(length = 100)
    private String region; // Ej: "Región del Biobío"

    @Column(length = 50)
    private String telefono; // Ej: "+56 41 234 5678"

    @Column(name = "horario_atencion", length = 150)
    private String horarioAtencion; // Ej: "Lunes a Viernes 09:00 a 18:30"

    @Column(name = "estado_operativo", nullable = false, length = 20)
    @Builder.Default
    private String estadoOperativo = "ACTIVO"; // ACTIVO, INACTIVO
}
