package cl.bookpointchile.ventas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// Línea de detalle enviada a ms-facturacion para que el documento tributario
// quede emitido con el producto (identificado por su productoId) y su cantidad.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleDocumentoRequestDTO {
    private Long productoId;
    private String productoNombre;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}
