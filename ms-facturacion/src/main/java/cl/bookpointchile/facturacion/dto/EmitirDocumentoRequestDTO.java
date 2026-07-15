package cl.bookpointchile.facturacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmitirDocumentoRequestDTO {

    @NotBlank(message = "El folio de venta es obligatorio")
    private String folioVenta;

    private Long ventaId; // Si no viene, se resuelve consultando ms-ventas por folio

    private Long usuarioId; // Si no viene, se resuelve consultando ms-ventas por folio

    private Long sucursalId; // Si no viene, se resuelve consultando ms-ventas por folio

    @NotBlank(message = "El RUT del cliente es obligatorio")
    private String rutCliente;

    private String razonSocial; // Requerido solo si tipoDocumento es "FACTURA"

    private String giro; // Requerido solo si tipoDocumento es "FACTURA"

    @NotBlank(message = "El tipo de documento es obligatorio (BOLETA o FACTURA)")
    private String tipoDocumento; // "BOLETA" o "FACTURA"

    @NotNull(message = "El monto neto es obligatorio")
    @Positive(message = "El monto neto debe ser un valor positivo")
    private Double montoNeto;

    // Líneas del documento, cada una identificando el producto por su código (productoId).
    // Si no vienen, se resuelven consultando el detalle de la venta en ms-ventas.
    @Valid
    private List<DetalleDocumentoRequestDTO> detalles;
}
