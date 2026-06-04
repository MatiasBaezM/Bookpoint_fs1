package cl.bookpointchile.facturacion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "documentos_tributarios",
    uniqueConstraints = @UniqueConstraint(name = "uk_documento_folio_venta", columnNames = "folio_venta")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoTributario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "folio_venta", nullable = false, unique = true, length = 50)
    private String folioVenta; // Identificador único de la venta que se está facturando

    @Column(name = "rut_cliente", nullable = false, length = 20)
    private String rutCliente; // RUT del cliente receptor (ej. "12.345.678-9")

    @Column(name = "razon_social", length = 150)
    private String razonSocial; // Razón Social (Requerido para FACTURA)

    @Column(length = 150)
    private String giro; // Giro del negocio (Requerido para FACTURA)

    @Column(name = "tipo_documento", nullable = false, length = 20)
    private String tipoDocumento; // "BOLETA" o "FACTURA"

    @Column(name = "monto_neto", nullable = false)
    private Double montoNeto; // Monto Neto ingresado

    @Column(name = "monto_iva", nullable = false)
    private Double montoIva; // 19% del neto (calculado)

    @Column(name = "monto_total", nullable = false)
    private Double montoTotal; // Neto + IVA (calculado)

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision; // Fecha y hora de emisión del documento
}
