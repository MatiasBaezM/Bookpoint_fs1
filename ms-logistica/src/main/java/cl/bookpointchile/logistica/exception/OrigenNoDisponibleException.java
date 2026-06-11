package cl.bookpointchile.logistica.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class OrigenNoDisponibleException extends RuntimeException {
    public OrigenNoDisponibleException(String message) {
        super(message);
    }
}
