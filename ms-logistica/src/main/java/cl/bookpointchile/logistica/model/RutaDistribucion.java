package cl.bookpointchile.logistica.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rutas_distribucion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RutaDistribucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String origen; // e.g. "Bodega Central Concepción"

    @Column(nullable = false, length = 100)
    private String destino; // e.g. "Hualpén", "Talcahuano", "Temuco"

    @Column(nullable = false, length = 100)
    private String transportista;

    @OneToMany(mappedBy = "rutaDistribucion", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Envio> envios = new ArrayList<>();
}
