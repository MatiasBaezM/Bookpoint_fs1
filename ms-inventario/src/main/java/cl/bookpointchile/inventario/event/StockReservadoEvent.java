package cl.bookpointchile.inventario.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservadoEvent implements Serializable {
    private Long ventaId;
    private String folio;
}
