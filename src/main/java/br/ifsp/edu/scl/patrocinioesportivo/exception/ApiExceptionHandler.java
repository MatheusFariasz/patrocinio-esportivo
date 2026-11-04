package br.ifsp.edu.scl.patrocinioesportivo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiException> authentication(AuthenticationException e) {
        return error("Credenciais inválidas.", UNAUTHORIZED, e);
    }

    @ExceptionHandler({ContratoInexistenteError.class, ClubeInexistenteError.class,
            PatrocinadorInexistenteError.class, ParcelaInexistenteError.class})
    public ResponseEntity<ApiException> notFound(RuntimeException e) {
        return error(e.getMessage(), NOT_FOUND, e);
    }

    @ExceptionHandler(PermissaoNegadaError.class)
    public ResponseEntity<ApiException> forbidden(PermissaoNegadaError e) {
        return error(e.getMessage(), FORBIDDEN, e);
    }

    @ExceptionHandler({OperacaoRedundanteError.class, TransicaoDeStatusInvalidaError.class,
            ContratoNaoAtivoError.class, PendenciaFinanceiraError.class, ParcelaDuplicadaError.class,
            PagamentoJaRegistradoError.class, EntityAlreadyExistsException.class})
    public ResponseEntity<ApiException> conflict(RuntimeException e) {
        return error(e.getMessage(), CONFLICT, e);
    }

    @ExceptionHandler({ValorInvalidoError.class, PeriodoInvalidoError.class, MetaInvalidaError.class,
            MetaObrigatoriaError.class, IdentificacaoObrigatoriaError.class, ClubeObrigatorioError.class,
            PatrocinadorObrigatorioError.class, PartesIguaisError.class, IllegalArgumentException.class})
    public ResponseEntity<ApiException> invalid(RuntimeException e) {
        return error(e.getMessage(), BAD_REQUEST, e);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiException> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + ": " + field.getDefaultMessage())
                .sorted().collect(Collectors.joining("; "));
        return error(message, BAD_REQUEST, e);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiException> malformed(Exception e) {
        return error("Requisição inválida. Verifique o JSON, as datas e os identificadores.", BAD_REQUEST, e);
    }

    private ResponseEntity<ApiException> error(String message, HttpStatus status, Exception e) {
        return ResponseEntity.status(status).body(new ApiException(message, status,
                ZonedDateTime.now(ZoneOffset.UTC), e.getClass().getSimpleName()));
    }
}
