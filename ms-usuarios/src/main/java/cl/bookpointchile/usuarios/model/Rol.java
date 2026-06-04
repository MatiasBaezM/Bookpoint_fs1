package cl.bookpointchile.usuarios.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre; // e.g. "Administrador del Sistema", "Jefe de Sucursal", "Asistente de Ventas", "Cliente Web"

    @Column(length = 250)
    private String descripcion;
}
