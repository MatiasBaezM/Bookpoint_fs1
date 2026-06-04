package cl.bookpointchile.proveedores.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "proveedores",
    uniqueConstraints = @UniqueConstraint(name = "uk_proveedor_rut", columnNames = "rut")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String rut;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(name = "email_contacto", nullable = false, length = 100)
    private String emailContacto;

    @Column(length = 20)
    private String telefono;

    @OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrdenCompra> ordenes = new ArrayList<>();
}
