package cl.bookpointchile.ventas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Petición síncrona a ms-inventario para descontar el stock de la sucursal donde
// se realizó la venta (reemplaza el antiguo mensaje asíncrono VentaCreadaEvent).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DescontarStockRequestDTO {
    private Long ventaId;
    private String folio;
    private Long usuarioId;
    private Long sucursalId;
    private List<DetalleStockRequestDTO> detalles;
}
