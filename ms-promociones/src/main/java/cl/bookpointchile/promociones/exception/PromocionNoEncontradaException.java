package cl.bookpointchile.promociones.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PromocionNoEncontradaException extends RuntimeException {
    public PromocionNoEncontradaException(String message) {
        super(message);
    }
}
