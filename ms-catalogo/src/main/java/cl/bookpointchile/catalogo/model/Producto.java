package cl.bookpointchile.catalogo.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "productos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 100)
    private String autor;

    @Column(nullable = false, length = 100)
    private String editorial;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(length = 1000)
    private String descripcion;

    @Column(nullable = false, length = 50)
    private String categoria; // e.g. "Libro", "Papelería", "Material Educativo"

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Resena> resenas = new ArrayList<>();

    // Sincronización bidireccional
    public void addResena(Resena resena) {
        resenas.add(resena);
        resena.setProducto(this);
    }

    public void removeResena(Resena resena) {
        resenas.remove(resena);
        resena.setProducto(null);
    }
}
