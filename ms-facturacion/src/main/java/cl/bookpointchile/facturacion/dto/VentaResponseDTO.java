package cl.bookpointchile.facturacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaResponseDTO {
    private Long id;
    private String folio;
    private Double total;
    private Long sucursalId;
    private Long usuarioId;
    private String clienteRut;
    private String tipoDocumento;
    private String estado;
    private List<DetalleVentaResponseDTO> detalles;

    // Vista parcial del detalle de la venta expuesto por ms-ventas
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleVentaResponseDTO {
        private Long productoId;
        private String productoNombre;
        private Integer cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;
    }
}
