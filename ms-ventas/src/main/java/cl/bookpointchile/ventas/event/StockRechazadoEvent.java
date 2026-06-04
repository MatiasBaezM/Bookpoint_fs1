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
public class StockRechazadoEvent implements Serializable {
    private Long ventaId;
    private String folio;
    private String motivo;
}
