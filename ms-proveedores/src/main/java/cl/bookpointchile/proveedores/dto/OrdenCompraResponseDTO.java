package cl.bookpointchile.proveedores.dto;

import cl.bookpointchile.proveedores.model.EstadoOrden;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraResponseDTO {
    private Long id;
    private Long proveedorId;
    private String proveedorRazonSocial;
    private LocalDateTime fechaEmision;
    private EstadoOrden estado;
    private BigDecimal total;
    private List<DetalleOrdenResponseDTO> detalles;
}
