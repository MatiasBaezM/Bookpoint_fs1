package cl.bookpointchile.sucursales.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SucursalNoEncontradaException extends RuntimeException {
    public SucursalNoEncontradaException(String message) {
        super(message);
    }
}
