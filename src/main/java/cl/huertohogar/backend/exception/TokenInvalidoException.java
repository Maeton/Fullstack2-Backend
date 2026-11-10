package cl.huertohogar.backend.exception;

public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException(String message) {
        super(message);
    }
}
