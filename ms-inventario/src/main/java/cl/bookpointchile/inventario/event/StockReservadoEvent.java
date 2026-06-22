package cl.bookpointchile.inventario.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservadoEvent {
    private Long ventaId;
    private String folio;
}
