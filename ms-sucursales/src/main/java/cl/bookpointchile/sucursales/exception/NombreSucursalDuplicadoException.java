package cl.bookpointchile.sucursales.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class NombreSucursalDuplicadoException extends RuntimeException {
    public NombreSucursalDuplicadoException(String message) {
        super(message);
    }
}
