package cl.huertohogar.backend.exception;

public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }

    public RecursoNoEncontradoException(String recurso, Long id) {
        super(recurso + " no encontrado con id " + id);
    }
}
