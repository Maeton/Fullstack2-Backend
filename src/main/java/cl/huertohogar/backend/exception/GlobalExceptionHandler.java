package cl.huertohogar.backend.exception;

import cl.huertohogar.backend.dto.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(RecursoNoEncontradoException ex,
                                                        HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(ReglaNegocioException ex,
                                                        HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictoException ex,
                                                        HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        Map<String, String> detalles = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            detalles.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "La solicitud contiene datos inválidos",
                request.getRequestURI(),
                detalles
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonInvalido(HttpMessageNotReadableException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no es un JSON válido o tiene un formato incorrecto",
                request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.CONFLICT,
                "No se pudo completar la operación: los datos están duplicados o en uso", request);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ErrorResponse> handleAccesoDenegado(AccesoDenegadoException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN,
                "No tienes permisos para realizar esta acción", request);
    }

    @ExceptionHandler(TokenInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleToken(TokenInvalidoException ex,
                                                     HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Credenciales inválidas o sesión expirada", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex,
                                                       HttpServletRequest request) {
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado en el servidor", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}
