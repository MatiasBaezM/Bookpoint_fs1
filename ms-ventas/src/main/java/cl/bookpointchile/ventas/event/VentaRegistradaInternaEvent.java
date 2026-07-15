package cl.bookpointchile.ventas.event;

import cl.bookpointchile.ventas.dto.EmitirDocumentoRequestDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class VentaRegistradaInternaEvent extends ApplicationEvent {
    private final EmitirDocumentoRequestDTO request;

    public VentaRegistradaInternaEvent(Object source, EmitirDocumentoRequestDTO request) {
        super(source);
        this.request = request;
    }
}
