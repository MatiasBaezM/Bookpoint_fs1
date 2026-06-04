package cl.bookpointchile.promociones.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PromocionCaducadaException extends RuntimeException {
    public PromocionCaducadaException(String message) {
        super(message);
    }
}
