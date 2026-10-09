package br.ifsp.edu.scl.patrocinioesportivo.exception;

import org.springframework.security.core.AuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.springframework.http.HttpStatus.*;

@ControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiException> handleAuthenticationException(AuthenticationException e) {
        return ResponseEntity.status(UNAUTHORIZED).body(new ApiException("Invalid credentials", UNAUTHORIZED,
                ZonedDateTime.now(ZoneId.of("Z")), e.getClass().getName()));
    }

    @ExceptionHandler(value = NullPointerException.class)
    public ResponseEntity<?> handleNullPointerException(NullPointerException e){
        final HttpStatus badRequest = BAD_REQUEST;
        final ApiException apiException = new ApiException(e.getMessage(), badRequest, ZonedDateTime.now(ZoneId.of("Z")),
                e.getClass().getName());
        return new ResponseEntity<>(apiException, badRequest);
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e){
        final HttpStatus badRequest = BAD_REQUEST;
        final ApiException apiException = new ApiException(e.getMessage(), badRequest, ZonedDateTime.now(ZoneId.of("Z")),
                e.getClass().getName());
        return new ResponseEntity<>(apiException, badRequest);
    }

    @ExceptionHandler(value = IllegalStateException.class)
    public ResponseEntity<?> handleIllegalStateException(IllegalStateException e){
        final HttpStatus forbidden = FORBIDDEN;
        final ApiException apiException = new ApiException(e.getMessage(), forbidden, ZonedDateTime.now(ZoneId.of("Z")),
                e.getClass().getName());
        return new ResponseEntity<>(apiException, forbidden);
    }

    @ExceptionHandler(value = EntityAlreadyExistsException.class)
    public ResponseEntity<?> handleEntityAlreadyExistsException(EntityAlreadyExistsException e){
        final HttpStatus conflict = CONFLICT;
        final ApiException apiException = new ApiException(e.getMessage(), conflict, ZonedDateTime.now(ZoneId.of("Z")),
                e.getClass().getName());
        return new ResponseEntity<>(apiException, conflict);
    }
}
