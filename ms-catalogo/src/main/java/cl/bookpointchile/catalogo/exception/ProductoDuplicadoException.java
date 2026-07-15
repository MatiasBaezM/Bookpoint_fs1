package cl.bookpointchile.catalogo.exception;

public class ProductoDuplicadoException extends RuntimeException {
    public ProductoDuplicadoException(String message) {
        super(message);
    }
}
