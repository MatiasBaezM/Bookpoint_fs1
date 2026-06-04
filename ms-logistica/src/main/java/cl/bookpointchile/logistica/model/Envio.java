package cl.bookpointchile.logistica.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "envios",
    uniqueConstraints = @UniqueConstraint(name = "uk_envio_venta", columnNames = "venta_id")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Envio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "venta_id", nullable = false)
    private Long ventaId;

    @Column(name = "direccion_destino", nullable = false, length = 250)
    private String direccionDestino;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoEnvio estado = EstadoEnvio.PENDIENTE;

    @Column(name = "fecha_estimada")
    private LocalDate fechaEstimada;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ruta_id")
    private RutaDistribucion rutaDistribucion;
}
