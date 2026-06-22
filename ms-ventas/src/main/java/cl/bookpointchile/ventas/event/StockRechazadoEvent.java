package cl.bookpointchile.ventas.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockRechazadoEvent {
    private Long ventaId;
    private String folio;
    private String motivo;
}
