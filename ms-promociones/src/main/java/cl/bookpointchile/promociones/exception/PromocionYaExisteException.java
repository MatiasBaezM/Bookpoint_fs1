package cl.bookpointchile.promociones.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PromocionYaExisteException extends RuntimeException {
    public PromocionYaExisteException(String message) {
        super(message);
    }
}
