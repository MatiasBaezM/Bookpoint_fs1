package cl.bookpointchile.ventas.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleVentaEvent {
    private Long productoId;
    private Integer cantidad;
}
