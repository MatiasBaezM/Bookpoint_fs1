package cl.bookpointchile.ventas.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleVentaEvent implements Serializable {
    private Long productoId;
    private Integer cantidad;
}
