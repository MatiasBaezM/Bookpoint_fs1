package cl.bookpointchile.proveedores.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProveedorNoEncontradoException extends RuntimeException {
    public ProveedorNoEncontradoException(String message) {
        super(message);
    }
}
