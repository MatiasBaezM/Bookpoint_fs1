package cl.bookpointchile.bodega.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class OrdenPickingNoEncontradaException extends RuntimeException {
    public OrdenPickingNoEncontradaException(String message) {
        super(message);
    }
}
