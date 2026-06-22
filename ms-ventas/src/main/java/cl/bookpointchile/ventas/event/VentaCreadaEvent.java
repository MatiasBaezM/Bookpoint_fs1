package cl.bookpointchile.ventas.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaCreadaEvent {
    private Long ventaId;
    private String folio;
    private List<DetalleVentaEvent> detalles;
}
