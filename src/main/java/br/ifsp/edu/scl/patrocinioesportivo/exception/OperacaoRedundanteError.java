package br.ifsp.edu.scl.patrocinioesportivo.exception;

public class OperacaoRedundanteError extends RuntimeException {

    public OperacaoRedundanteError(String message) {
        super(message);
    }
}