package cl.bookpointchile.bodega.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaResponseDTO {
    private Long id;
    private String folio;
    private LocalDateTime fecha;
    private String tipoVenta;
    private String estado;
    private Long usuarioId;
    private String clienteNombre;
    private String clienteRut;
    private String asistenteNombre;
    private BigDecimal subtotal;
    private BigDecimal descuentoAplicado;
    private String tipoDescuento;
    private String codigoDescuento;
    private BigDecimal total;
    private String tipoDocumento;
    private String razonSocial;
    private String giro;
}
